# Notas de los talleres — rica-api

## Taller Lección 4 · Hexagonal en rica-api

### Paso 1 · Auditoría de puertos y adaptadores existentes

- **`InvestigadorRepository` ¿puerto primario o secundario?**
  Secundario: es el núcleo (`InvestigadorService`) quien inicia la llamada hacia algo externo (la base de datos), no al revés.
  El problema es que lo diseñó la tecnología (extiende `JpaRepository` y hereda ~30 métodos) y no el caso de uso.

- **`InvestigadorController` ¿adaptador primario o secundario?**
  Primario: recibe la petición desde fuera (un cliente HTTP) y la traduce a una llamada al núcleo. Envuelve Spring MVC / HTTP + JSON.

- **¿Qué falta para que exista un puerto primario explícito?**
  Una interfaz que declare lo que el caso de uso promete (`InvestigadorUseCase`). Hoy el controlador depende de la clase
  concreta `InvestigadorService`, así que no hay contrato entre "lo que se promete" y "cómo se cumple".

- **`InvestigadorFactory` ¿núcleo o adaptador?**
  Núcleo: solo importa clases del dominio (`Investigador`, `CorreoInstitucional`, `CorreoDuplicadoException`).
  La única "fuga" era la dependencia directa de `InvestigadorRepository` (Spring Data); tras el paso 3 depende del puerto
  `RepositorioInvestigadores`, así que queda limpio. La anotación `@Component` es la única concesión a Spring.

### Resultado

| Paquete | Clases | Rol |
|---|---|---|
| `investigadores.dominio` | `Investigador`, `CorreoInstitucional`, `CorreoDuplicadoException` | Núcleo: entidad, Value Object y regla de negocio |
| `investigadores.aplicacion` | `InvestigadorUseCase`, `RepositorioInvestigadores`, `ReportadorMinCiencias`, `InvestigadorService`, `InvestigadorFactory` | Núcleo: puertos y casos de uso |
| `investigadores.infraestructura.entrada.web` | `InvestigadorController`, `InvestigadorRequest`, `InvestigadorResponse`, `InvestigadorMapper` | Adaptador primario HTTP |
| `investigadores.infraestructura.salida.persistencia` | `InvestigadorRepository`, `InvestigadorRepositoryJpaAdapter` | Adaptador secundario Spring Data JPA |
| `investigadores.infraestructura.salida.reportes` | `ClienteReportesMinCiencias` | Adaptador secundario simulado (reto opcional A) |

- `InvestigadorServiceConFalsoTest` construye el núcleo con `RepositorioInvestigadoresFalso` (un `Map` en memoria), sin
  `@SpringBootTest`, sin `@Mock` y sin base de datos: el núcleo solo necesita algo que cumpla el contrato del puerto.
- Pendiente detectado: `PublicacionService` todavía importa `InvestigadorRepository` de la infraestructura de otro
  subdominio. Es exactamente la costura que se corta en el Taller de la Lección 5.

## Taller Lección 5 · Descomposición real de rica-api

### Paso 1 · El corte: qué va en cada servicio

| Paquete / clase actual | Destino |
|---|---|
| `investigadores.*` (dominio, aplicación, infraestructura) | `investigadores-service` |
| `publicaciones.*` | `publicaciones-service` |
| `compartido.GlobalExceptionHandler`, `RecursoNoEncontradoException` | Se duplica en ambos: cada servicio tiene su propio manejo de errores |
| `plataforma.StatusController`, `CorsConfig` | Se duplica en ambos, adaptado (cada servicio expone su propio `/api/status`) |
| `plataforma.ArranqueInformativo`, `SaludoInstitucionalService` | Se descarta: era una demostración de IoC/DI sin valor de negocio |

**Sobre la duplicación.** Es la primera consecuencia real de *Database per Service* y despliegue independiente.
Compartir un `@ControllerAdvice` exigiría una librería común versionada aparte (un *shared kernel*). Para dos
servicios pequeños la duplicación es más barata que el acoplamiento de versiones; si el número de servicios crece,
vale la pena extraer un módulo `rica-commons`.

### Decisiones tomadas durante el corte

- `LimitePublicacionesAnualesService` sí cruzaba dominios: recibía la entidad `Investigador`. En
  `publicaciones-service` pasa a recibir `(investigadorCorreo, anio)`, que es todo lo que la regla necesita.
  La regla ahora se aplica de verdad en `PublicacionService.registrar` y responde **422** con `LimiteAnualExcedidoException`.
- `PublicacionService` ya no conoce `InvestigadorRepository`: depende del puerto `VerificadorInvestigador`.
  El adaptador `VerificadorInvestigadorPendiente` acepta cualquier correo a propósito: la dependencia entre dominios
  no desapareció, se hizo **visible**. Se resuelve en el Taller de la Lección 6.
- `PublicacionResponse` ahora incluye el `id` (antes la cabecera `Location` apuntaba a un id que el cuerpo no mostraba).
- Monorepo: los dos servicios viven en `rica-microservicios/` dentro de este mismo repositorio de GitHub, cada uno con
  su propio `pom.xml`, Maven Wrapper y `docker-compose.yml`.

### Evidencia (paso 5)

```text
GET  :8080/api/status        -> {"servicio":"investigadores-service","estado":"operativo"}
GET  :8081/api/status        -> {"servicio":"publicaciones-service","estado":"operativo"}
POST :8080/api/investigadores -> 201
POST :8081/api/publicaciones (correo inventado) -> 201   <- brecha esperada, la cierra la Lección 6
```
