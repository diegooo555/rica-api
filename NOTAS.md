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

## Taller Lección 6 · Comunicación entre microservicios

### Qué se construyó

| Necesidad | Antes (un proceso) | Ahora |
|---|---|---|
| ¿Existe este investigador? | Llamada de método a `InvestigadorRepository` | gRPC `ExisteInvestigadorPorCorreo` protegido con Circuit Breaker |
| Notificar que se registró un investigador | No aplicaba | `investigadores.registrados` vía Transactional Outbox → Kafka |
| El frontend consulta ambos recursos | Dos llamadas directas a rica-api | Una sola entrada: Gateway en `:8000`, rutas `lb://` resueltas con Eureka |
| Eliminar un investigador con publicaciones | Transacción en memoria | Saga coreografiada con bloqueo semántico (`EN_ELIMINACION`) |

### Decisiones y por qué

- **gRPC para la verificación** (síncrona): `publicaciones-service` necesita la respuesta *antes* de guardar.
  Es tráfico interno entre servicios, con contrato tipado en `investigador.proto`. El frontend sigue usando REST.
  Se agregó la operación por **correo** (la guía solo traía la de id) porque es el dato que tiene una publicación.
- **El adaptador gRPC usa el puerto primario** `InvestigadorUseCase`, no el repositorio: un adaptador de entrada no
  debe saltarse la capa de aplicación (igual que el controlador REST).
- **Circuit Breaker (Resilience4j, vía Spring Cloud CircuitBreaker)**: ventana de 10 llamadas, se abre con 50 % de
  fallos, espera 10 s, plazo gRPC de 2 s. Con el circuito abierto la respuesta es **503 en milisegundos**.
  Se eligió **fallar cerrado** (no registrar) en lugar de aceptar la publicación sin verificar: es preferible
  pedir un reintento a guardar datos de un autor que quizá no existe.
- **Transactional Outbox**: el evento se guarda en `outbox_events` en la misma transacción que el investigador.
  Si Kafka está caído no se pierde nada; `RelayOutbox` lo publica en orden cuando vuelva (al menos una vez).
- **Saga coreografiada** en tres pasos; cada paso es idempotente (repetir un evento no rompe nada), porque Kafka
  entrega "al menos una vez". Mientras dura, el investigador está `EN_ELIMINACION`: no se borra, pero tampoco
  acepta publicaciones nuevas (el gRPC responde `existe=false`).
- **Gateway**: Spring Cloud Gateway (2025.1) usa ahora el prefijo `spring.cloud.gateway.server.webflux.routes`.
  El filtro `RequestRateLimiter` de la guía requiere Redis; se deja fuera para no agregar otra pieza de infraestructura.
- **Contrato duplicado**: cada servicio guarda su copia de `investigador.proto` con su propio `java_package`;
  lo que debe coincidir es el `package rica.investigadores` y los mensajes.

### Evidencia (todo por el Gateway, puerto 8000)

```text
POST /api/investigadores (Ana)                 -> 201 {"estado":"ACTIVO"}
POST /api/publicaciones (ana, x2)              -> 201 (gRPC confirmó al autor)
POST /api/publicaciones (inventado@uptc.edu.co)-> 404 No existe un investigador con correo ...
DELETE /api/investigadores/1                   -> 202
GET /api/investigadores/1 (inmediato)          -> 200 {"estado":"EN_ELIMINACION"}
GET /api/investigadores/1 (segundos después)   -> 404
GET /api/publicaciones?investigadorCorreo=ana  -> 200, ambas con "huerfana": true

Logs:
  Outbox: evento 1 publicado en investigadores.registrados
  Outbox: evento 2 publicado en investigadores.en-eliminacion
  Saga: 2 publicaciones de ana.torres@uptc.edu.co marcadas como huérfanas
  Saga: publicaciones confirmó para ana.torres@uptc.edu.co. Eliminando investigador.

Con investigadores-service detenido:
  POST /api/publicaciones -> 503 en ~5 ms
  CallNotPermittedException: CircuitBreaker 'investigadores' is OPEN and does not permit further calls
```

## Taller Lección 8 · CQRS: una ficha de investigador alimentada por eventos

### El plan

| Servicio | Lado | Qué hace |
|---|---|---|
| `investigadores-service` | Escritura | Publica `investigadores.registrados`, `.en-eliminacion` y ahora `.eliminados` (por el Outbox) |
| `publicaciones-service` | Escritura | Publica `publicaciones.registradas` solo después de guardar con éxito |
| `consultas-service` (nuevo) | Lectura | Escucha los cuatro eventos y mantiene `fichas_investigador` en su propia base `rica_consultas` |

### Evidencias

1. **Ficha con dos publicaciones**, sin que `consultas-service` llame a ningún otro servicio:
   ```json
   {"correo":"luis.mora@uptc.edu.co","nombreCompleto":"Luis Mora","estado":"ACTIVO","totalPublicaciones":2,
    "publicaciones":[{"titulo":"Microservicios en la UPTC",...},{"titulo":"CQRS en la practica",...}]}
   ```
2. **Ranking** (`/api/consultas/ranking?top=3`): `luis.mora 2`, `ana.torres 1`.
3. **Log** de `consultas-service`:
   ```text
   Proyección: ficha de luis.mora@uptc.edu.co actualizada (registro)
   Proyección: ficha de luis.mora@uptc.edu.co ahora tiene 2 publicaciones
   Proyección: luis.mora@uptc.edu.co marcado EN_ELIMINACION
   Proyección: ficha de luis.mora@uptc.edu.co eliminada
   ```
   Con `consultas-service` detenido se registró otra publicación (**201**); al arrancarlo, el total subió a 3 sin reenviar nada.
4. **Ficha eliminada tras la Saga**: `GET /api/consultas/investigadores/luis.mora@uptc.edu.co` → **404** "No hay ficha para el investigador".
5. **Reconstrucción** (colección borrada + `group-id: consultas-service-v2`): las fichas reaparecieron solas.
   Luis **resucitó** (`ACTIVO`, 3 publicaciones): en el log, `marcado EN_ELIMINACION` y `eliminada` se procesaron
   *antes* que `actualizada (registro)`. Es la trampa descrita en el taller: Kafka no garantiza orden entre tópicos
   distintos. En producción se evita publicando todos los eventos de un agregado en **un solo tópico con versión**.

### ¿Qué se vería distinto si lectura y escritura fueran el mismo modelo?

1. La ficha exigiría un *join* entre PostgreSQL (investigadores) y MongoDB (publicaciones) en cada consulta, cruzando dos servicios por red.
2. El ranking necesitaría contar publicaciones de todos los investigadores en cada petición en vez de ordenar un campo ya guardado.
3. Si el servicio de lectura cae, hoy la escritura sigue (201); con un solo modelo, una consulta pesada degradaría también los comandos.
4. La eliminación sería inmediata y transaccional, pero acoplaría los dos servicios en una transacción distribuida.
5. No se podría "borrar y reconstruir" la vista: el modelo único es la fuente de verdad.

### Notas técnicas

- Spring Boot 4 renombró `spring.data.mongodb.uri` a `spring.mongodb.uri`; se usa la clave nueva.
- Se usa Jackson 3 (`tools.jackson.databind.ObjectMapper`), el que autoconfigura Spring Boot 4.

## Taller Lección 9 · EDA: consumidores que resisten duplicados, errores y mensajes venenosos

### Evidencias

1. **Línea base (sin defensas)** — un mensaje `esto no es json` en `investigadores.registrados`:
   ```text
   ERROR o.s.kafka.listener.DefaultErrorHandler : Backoff FixedBackOffExecution[interval=0, currentAttempts=10, maxAttempts=9]
         exhausted for investigadores.registrados-0@2
   ```
   Diez intentos sin pausa (la partición detenida mientras tanto) y el mensaje se descarta sin rastro recuperable.
2. **Con `ManejoErroresConfig`** — el mismo mensaje va directo a la DLT, sin reintentos (es `JacksonException`):
   ```text
   kafka_dlt-original-topic:investigadores.registrados
   kafka_dlt-exception-fqcn:org.springframework.kafka.listener.ListenerExecutionFailedException
   kafka_dlt-exception-cause-fqcn:tools.jackson.core.exc.StreamReadException
   valor: esto no es json (3)
   ```
   **Hallazgo:** en Spring Kafka 4 el `DeadLetterPublishingRecoverer` usa por defecto el sufijo `-dlt`
   (el primer intento terminó en `investigadores.registrados-dlt`). Se fijó el nombre `<tópico>.DLT` con un
   *destination resolver* explícito para que coincida con el tópico declarado.
3. **Contador ingenuo** — `N = 2`, alta de Sofía → `N + 1 = 3`. Tras reiniciar el grupo `consultas-estadisticas`
   al inicio del tópico: **6**. Cada alta se contó dos veces sin una sola excepción en el log.
4. **Consumidor idempotente** — colecciones limpiadas, grupo al inicio: **3**. Repetido otra vez: **3**.
   ```text
   Estadísticas: evento investigadores.registrados-0-0 ya procesado, se ignora
   Estadísticas: evento investigadores.registrados-0-1 ya procesado, se ignora
   Estadísticas: mensaje no válido en investigadores.registrados-0@2, no se cuenta
   ```

### Paso 7 · ¿Qué hacemos con la DLT?

1. **¿Quién se entera y cómo?** El equipo dueño del consumidor (consultas-service). Una métrica con el tamaño/lag de
   `*.DLT` y una alerta cuando sea mayor que 0 durante más de unos minutos, visible en el tablero del servicio.
   El correo solo para la alerta, no por cada mensaje.
2. **¿Cómo se reprocesa?** Con una herramienta aparte (un pequeño consumidor o un endpoint de administración) que lee
   la DLT, permite revisar el mensaje y lo reenvía al tópico original una vez corregido el error. Reenviar a ciegas
   todo el contenido de la DLT repetiría los mensajes que siguen siendo inválidos.
3. **¿Cuándo descartar?** Cuando el mensaje es basura irrecuperable (como `esto no es json`) o el hecho ya no tiene
   efecto. Lo decide una persona del equipo dueño, dejando registro de qué se descartó y por qué.
4. **¿Y si es válido pero llegó a destiempo?** Reprocesarlo puede ser peligroso (p. ej. resucitar una ficha eliminada,
   como se vio en CQRS). Hace falta versión o marca de tiempo del evento para descartar lo que llegue después
   de un estado más nuevo; es justo lo que la observabilidad de la Lección 10 ayuda a diagnosticar.

### ¿Por qué reintentar un mensaje venenoso sin fin es peor que perderlo y peor que una DLT?

Reintentarlo sin fin bloquea la partición: ningún mensaje posterior se procesa, así que un solo mensaje roto detiene
todo el flujo, cosa que perderlo no hace. La DLT da lo mejor de ambos: el consumidor sigue de inmediato y el mensaje
queda guardado con su diagnóstico para revisarlo y, si corresponde, reprocesarlo.

### Límites conocidos

- Los ids por posición no detectan duplicados del **productor** (dos publicaciones del mismo hecho tienen offsets
  distintos). Para eso el evento debe llevar su propio id de negocio, por ejemplo el id del Outbox.
- Registrar el id y sumar son dos escrituras: si el proceso cae entre ambas se pierde un conteo.
