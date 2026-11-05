# rica-microservicios

Monorepo con los servicios en que se dividió `rica-api`.

| Servicio | Puerto | Infraestructura | Rol |
|---|---|---|---|
| `eureka-server` | 8761 | — | Registro y descubrimiento de servicios |
| `gateway` | 8000 | — | Punto de entrada único (Spring Cloud Gateway) |
| `investigadores-service` | 8080 (HTTP), 9090 (gRPC) | PostgreSQL + Kafka | Investigadores, Outbox, Saga de eliminación |
| `publicaciones-service` | 8081 | MongoDB | Publicaciones, cliente gRPC con Circuit Breaker |

## Comunicación

- **Síncrona (gRPC):** `publicaciones-service` pregunta a `investigadores-service` si un correo pertenece a un
  investigador activo antes de registrar una publicación. La llamada pasa por un Circuit Breaker (Resilience4j):
  si el otro servicio cae, responde 503 de inmediato en lugar de esperar el timeout.
- **Asíncrona (Kafka):** `investigadores-service` guarda sus eventos en una tabla *outbox* dentro de la misma
  transacción y un relay los publica en Kafka.

| Tópico | Publica | Consume |
|---|---|---|
| `investigadores.registrados` | investigadores (outbox) | — |
| `investigadores.en-eliminacion` | investigadores (outbox) | publicaciones |
| `publicaciones.marcadas-huerfanas` | publicaciones | investigadores |

### Saga de eliminación (coreografía)

1. `DELETE /api/investigadores/{id}` → el investigador queda `EN_ELIMINACION` (bloqueo semántico: ya no acepta
   publicaciones) y se publica `investigadores.en-eliminacion`. Responde **202**.
2. `publicaciones-service` marca sus publicaciones como huérfanas y publica `publicaciones.marcadas-huerfanas`.
3. `investigadores-service` recibe la confirmación y borra al investigador.

## Cómo correrlo

```bash
cd investigadores-service && docker compose up -d && cd ..   # PostgreSQL + Kafka
cd publicaciones-service && docker compose up -d && cd ..    # MongoDB
```

Luego, cada uno en su terminal y en este orden: `eureka-server`, `investigadores-service`,
`publicaciones-service`, `gateway` (`./mvnw spring-boot:run` dentro de cada carpeta).

La colección de Bruno está en [`bruno/RICA-Microservicios`](bruno/RICA-Microservicios) y apunta al Gateway.
