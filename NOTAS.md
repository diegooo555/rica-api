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

## Taller Lección 10 · Observabilidad: seguir una Saga con un id de correlación

### Línea base (paso 2)

Antes del cambio, al eliminar un investigador las tres terminales escribían líneas de la Saga sin nada que dijera a
qué petición pertenecía cada una. Con diez eliminaciones simultáneas sería imposible separarlas.

### Evidencias

1. **`/actuator/health`** — los tres servicios `UP`:
   - investigadores (8080): componentes `db` (PostgreSQL), `discoveryComposite`, `diskSpace`, `ping`, …
   - publicaciones (8081) y consultas (8083): componente `mongo`, `discoveryComposite`, …
2. **Logs con `demo-001`** (`DELETE /api/investigadores/{id}` por el Gateway con `X-Correlation-Id: demo-001`,
   la respuesta devuelve la misma cabecera):
   ```text
   [investigadores-service,demo-001] Saga: inicia la eliminación de mateo.diaz@uptc.edu.co
   [publicaciones-service,demo-001]  Saga: 2 publicaciones de mateo.diaz@uptc.edu.co marcadas como huérfanas
   [investigadores-service,demo-001] Saga: publicaciones confirmó para mateo.diaz@uptc.edu.co. Eliminando investigador.
   ```
   **Brecha en consultas-service:** sus líneas salían como `[consultas-service,]`, porque `ProyeccionListener` no
   leía el id del mensaje. **Cambio que la cierra (aplicado):** el mismo patrón del paso 5 — leer `correlationId`
   del JSON y envolver la proyección en `MDC.putCloseable(...)`. Un mensaje ilegible se deja pasar sin id para que
   el proyector falle y vaya a la DLT como antes. Resultado con `demo-002`:
   ```text
   [consultas-service,demo-002] Proyección: valeria.pinto@uptc.edu.co marcado EN_ELIMINACION
   [consultas-service,demo-002] Proyección: ficha de valeria.pinto@uptc.edu.co eliminada
   ```
3. **Métricas** (`/actuator/metrics/http.server.requests` de investigadores-service, método RED):
   `COUNT=10`, `TOTAL_TIME=0.4885 s`, `MAX=0.2673 s`; con `?tag=status:404`: `COUNT=1`.
   En Spring Boot 4.1.1 `/actuator/metrics` respondió sin agregar `spring-boot-starter-micrometer-metrics`.

   **Lag** del grupo `consultas-service-v2` en `investigadores.registrados`:

   | Momento | CURRENT-OFFSET | LOG-END-OFFSET | LAG |
   |---|---|---|---|
   | Al día | 10 | 10 | 0 |
   | consultas detenido + 3 altas | 10 | 13 | **3** |
   | Tras arrancar | 13 | 13 | 0 |
4. **Saga atascada** (`publicaciones-service` detenido, eliminación con `demo-002`):
   - `GET /api/consultas/investigadores?estado=EN_ELIMINACION` → `valeria.pinto@uptc.edu.co EN_ELIMINACION`
   - Lag de `publicaciones-service` en `investigadores.en-eliminacion`: **1**
   - Al arrancar `publicaciones-service` la Saga continuó sola; ficha e investigador → 404, y `demo-002` aparece
     en los tres servicios: el id sobrevivió a la pausa porque viaja dentro del evento guardado en Kafka.

### Paso 9 · Discusión: ¿y si nunca vuelve?

1. **Estado y quién se entera.** El investigador quedaría para siempre `EN_ELIMINACION`: ni se borra ni acepta
   publicaciones (el bloqueo semántico se vuelve permanente). Hoy nadie se daría cuenta salvo que alguien mire el lag
   o la consulta de fichas en ese estado; el usuario solo vería que el investigador "no termina de borrarse".
2. **Compensación.** `publicaciones-service` publicaría `publicaciones.marcado-fallido` (con el correo y el
   `correlationId`); lo consumiría `investigadores-service`, que devolvería al investigador a `ACTIVO` y publicaría
   `investigadores.eliminacion-cancelada` para que consultas-service también revierta la ficha. Sería idempotente:
   volver a `ACTIVO` a alguien que ya está `ACTIVO` no cambia nada, y solo se compensa si sigue `EN_ELIMINACION`.
3. **Métrica y alerta.** Un *gauge* `rica.saga.eliminacion.atascadas` = investigadores en `EN_ELIMINACION` desde
   hace más de 10 minutos (requiere guardar la fecha de inicio). Alerta si es mayor que 0 durante 5 minutos,
   junto con una alerta de lag > 0 sostenido en el grupo `publicaciones-service`.
4. **¿Coreografía u orquestación con cuatro pasos más?** Orquestada. Con siete pasos el flujo repartido entre
   listeners se vuelve difícil de seguir y de compensar; un orquestador (en investigadores-service) guarda el estado
   de cada Saga, sabe qué paso falló y qué compensar, y da un único lugar para medir duración y atascos.

## Taller Lección 11 · Contenedores y CI real para RICA

### Paso 2 · Preguntas sobre el Dockerfile

- **(a) ¿Por qué primero `pom.xml` y luego `src`?** Docker cachea cada capa y la invalida solo si cambian sus
  entradas. Las dependencias dependen del `pom.xml`, que cambia poco; el código cambia todo el tiempo. Copiando
  primero el pom, `dependency:go-offline` queda en caché y un cambio de código solo recompila.
- **(b) ¿Qué contiene la imagen final y qué no?** Solo el JRE 25 de Temurin, un usuario sin privilegios y el `.jar`.
  No tiene Maven, ni el JDK, ni el código fuente, ni el repositorio local de dependencias: todo eso quedó en la
  etapa `build`, que se descarta.
- **(c) ¿Por qué `USER rica`?** Si alguien explota una vulnerabilidad de la aplicación, obtiene los permisos del
  proceso. Corriendo como un usuario de sistema sin privilegios, no es root dentro del contenedor.

### Evidencias

1. **Imagen** — `docker images rica/eureka-server` → `rica/eureka-server:0.1  589MB`.
   En `docker history` la capa del `.jar` es `COPY /app/target/*.jar app.jar  60.7MB`.
2. **Panel de Eureka en contenedor** — `http://localhost:8761` respondió `<title>Eureka</title>` y los cuatro
   servicios (gateway, investigadores, publicaciones, consultas) se registraron en el contenedor.
3. **Caché de capas**

   | Construcción | Tiempo | Qué se ejecutó |
   |---|---|---|
   | Primera | 99 s | Todo: descarga de dependencias y compilación |
   | Segunda, sin cambios | ~0 s | Nada: 8 capas `CACHED` |
   | Tercera, cambio en `application.yml` | 6 s | Solo `COPY src ./src` y `mvn package` (y la copia del jar); `COPY pom.xml` y `dependency:go-offline` salieron `CACHED` |

   **Replicación contra sí mismo:** el log del contenedor mostraba `Adding new peer nodes [http://localhost:8761/eureka/]`
   y `Replica node URL: http://localhost:8761/eureka/`: Eureka se tomaba como su propio par. En los ~3 minutos que
   corrió no alcanzaron a aparecer los `SocketTimeoutException`, pero la configuración que los provoca estaba ahí.
   Con `eureka.instance.hostname: localhost` esas líneas desaparecen.
   **Compose:** `docker compose up -d --build` reutilizó las 8 capas (`CACHED`) y arrancó en ~3 s.
   **Variables de entorno:** la misma imagen con `-e SERVER_PORT=9000` sirvió el panel en `:9000` con
   "No instances available" (los servicios siguen apuntando al 8761).
4. **GitHub Actions** (rama `taller-contenedores`):

   | Commit | Resultado | Detalle |
   |---|---|---|
   | `ci: probar los cinco servicios…` | ✅ success | 5 jobs en verde |
   | `test: romper la CI a proposito` | ❌ failure | Solo `Pruebas de consultas-service` en rojo; los otros cuatro en verde |
   | `Revert "test: romper la CI…"` | ✅ success | De vuelta a verde |

   Los `mvnw` de todos los servicios ya estaban guardados con LF; igual se dejó `.gitattributes` con
   `mvnw text eol=lf` para que un clon en Windows no los rompa.

### Paso 5 · ¿Por qué una sola imagen con configuración por entorno?

Lo que se prueba es exactamente lo que llega a producción: el mismo artefacto, byte a byte, cambiando solo variables.
Con una imagen por entorno, cada una se construye por separado y podría diferir (dependencias resueltas otro día,
otra versión del código), y un error de producción no se reproduce en pruebas. Además, los secretos y las URLs no
quedan horneados dentro de la imagen.

### Paso 6 · Caché a mano con Redis (cache-aside)

```text
> SET ficha:ana.torres@uptc.edu.co {"nombre":"Ana Torres","publicaciones":3} EX 30   -> OK
> GET ficha:ana.torres@uptc.edu.co                                                -> {"nombre":"Ana Torres","publicaciones":3}   (hit)
> TTL ficha:ana.torres@uptc.edu.co                                                -> 30
  (tras expirar) GET                                                              -> (nil)   (miss: leer de la base y volver a guardar)
> SET ... EX 300 ; DEL ficha:ana.torres@uptc.edu.co ; GET                          -> 1 ; (nil)   (invalidación al modificar)
```

**Ejercicio de decisión — RICA (módulo de investigadores y publicaciones):**

| Tipo de dato | Almacén | Por qué | Consulta principal que lo justifica | Costo de añadirlo |
|---|---|---|---|---|
| Investigadores | PostgreSQL | Datos estructurados con unicidad de correo y transacciones (Outbox en la misma transacción) | `SELECT … WHERE correo = ?` + `INSERT` del outbox atómico | Ninguno: ya existe |
| Publicaciones | MongoDB | Campos variables según el tipo (`detalles` de artículo, libro, ponencia…) | `find({investigadorCorreo})` y conteo por año | Ya existe; otra tecnología que operar y respaldar |
| Fichas de investigador (lectura) | MongoDB (`rica_consultas`) | Documento desnormalizado listo para la pantalla, se reconstruye desde eventos | `findById(correo)` y ranking ordenado por `totalPublicaciones` | Bajo: reutiliza la instancia de Mongo con otra base |
| Eventos entre servicios | Kafka | Log durable y reproducible: permite reconstruir el modelo de lectura y absorber caídas | Consumir `investigadores.*` desde el offset guardado o desde el inicio | Alto: un clúster más que operar y monitorear (lag, DLT) |
| Caché de fichas muy consultadas | Redis (solo si hiciera falta) | Lecturas en memoria con TTL e invalidación por evento | `GET ficha:{correo}` | Medio: otra pieza más, invalidación y consistencia que cuidar |
| Búsqueda por texto en títulos | PostgreSQL `tsvector` o índice de texto de Mongo | El volumen de RICA no justifica Elasticsearch | `WHERE to_tsvector(titulo) @@ plainto_tsquery(?)` | Bajo con el motor existente; alto con un motor dedicado |

**¿Hace falta más de una tecnología?** Para el tamaño de RICA, una relacional bien usada (PostgreSQL con `jsonb`
para los detalles y `tsvector` para búsquedas) bastaría para investigadores y publicaciones. MongoDB se justifica por
la variabilidad de las publicaciones y porque el modelo de lectura es un documento natural, y Kafka por la
comunicación asíncrona y la reconstrucción de vistas. Redis no se justifica hoy: `consultas-service` ya responde con
una sola lectura por clave; se agregaría solo si las mediciones mostraran latencia o carga real.

### Lectura opcional · ¿Qué haría falta para correr consultas-service en un contenedor?

1. Un `Dockerfile` igual al de `eureka-server`, con `EXPOSE 8083`.
2. Una red Docker compartida con Kafka, MongoDB y Eureka, y referirse a ellos por nombre de servicio, no `localhost`.
3. Kafka con dos listeners: uno interno anunciado como `rica-kafka:29092` para los contenedores y otro `localhost:9092` para el host.
4. Configuración por variables: `SPRING_MONGODB_URI=mongodb://publicaciones-mongo:27017/rica_consultas`,
   `SPRING_KAFKA_BOOTSTRAP_SERVERS=rica-kafka:29092`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://rica-eureka:8761/eureka`.
5. `depends_on` con *healthchecks* para que no arranque antes que Kafka y MongoDB, y `prefer-ip-address` para que el Gateway lo alcance.
