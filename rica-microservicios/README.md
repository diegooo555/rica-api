# rica-microservicios

Monorepo con los servicios en que se dividió `rica-api`.

| Servicio | Puerto | Base de datos |
|---|---|---|
| `investigadores-service` | 8080 | PostgreSQL (`rica`) |
| `publicaciones-service` | 8081 | MongoDB (`rica`) |

## Cómo correrlos

Cada servicio trae su propio `docker-compose.yml` con su base de datos:

```bash
cd investigadores-service
docker compose up -d
./mvnw spring-boot:run
```

```bash
cd publicaciones-service
docker compose up -d
./mvnw spring-boot:run
```
