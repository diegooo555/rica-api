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
