package com.rica.investigadoresservice.investigadores.infraestructura.salida.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lleva los eventos pendientes de la tabla outbox a Kafka, en orden.
 * Si Kafka no está disponible se detiene y lo vuelve a intentar en la siguiente pasada:
 * ningún evento se pierde, a lo sumo se publica más de una vez (entrega "al menos una vez").
 */
@Component
public class RelayOutbox {

    private static final Logger log = LoggerFactory.getLogger(RelayOutbox.class);
    private static final long ESPERA_CONFIRMACION_S = 5;

    private final OutboxEventRepository repositorio;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public RelayOutbox(OutboxEventRepository repositorio, KafkaTemplate<String, String> kafkaTemplate) {
        this.repositorio = repositorio;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${rica.outbox.intervalo-ms:1000}")
    public void publicarPendientes() {
        List<OutboxEvent> pendientes = repositorio.findTop100ByPublicadoEnIsNullOrderByIdAsc();
        for (OutboxEvent evento : pendientes) {
            try {
                kafkaTemplate.send(evento.getTopico(), evento.getClave(), evento.getPayload())
                        .get(ESPERA_CONFIRMACION_S, TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Outbox: no se pudo publicar el evento {} en {} ({}); se reintentará",
                        evento.getId(), evento.getTopico(), e.toString());
                return; // se respeta el orden: no se salta al siguiente evento
            }
            evento.marcarPublicado();
            repositorio.save(evento);
            log.info("Outbox: evento {} publicado en {}", evento.getId(), evento.getTopico());
        }
    }
}
