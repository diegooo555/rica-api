-- Transactional Outbox: los eventos se guardan en la misma transacción que el cambio de negocio
-- y un proceso aparte (RelayOutbox) los publica en Kafka.
CREATE TABLE outbox_events (
    id            BIGSERIAL PRIMARY KEY,
    topico        VARCHAR(150) NOT NULL,
    clave         VARCHAR(150),
    payload       TEXT         NOT NULL,
    creado_en     TIMESTAMP    NOT NULL,
    publicado_en  TIMESTAMP
);

CREATE INDEX idx_outbox_pendientes ON outbox_events (id) WHERE publicado_en IS NULL;
