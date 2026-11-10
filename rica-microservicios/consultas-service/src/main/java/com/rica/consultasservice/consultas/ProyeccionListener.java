package com.rica.consultasservice.consultas;

import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador de entrada del lado de lectura: escucha los eventos y delega en el proyector.
 * Si el evento trae un id de correlación, lo pone en el MDC mientras se proyecta, para que
 * la traza de la Saga también aparezca en los logs de consultas-service.
 */
@Component
public class ProyeccionListener {

    private final ProyectorFichas proyector;
    private final ObjectMapper objectMapper;

    public ProyeccionListener(ProyectorFichas proyector, ObjectMapper objectMapper) {
        this.proyector = proyector;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "investigadores.registrados")
    public void investigadorRegistrado(String mensaje) {
        conTraza(mensaje, () -> proyector.alRegistrarseInvestigador(mensaje));
    }

    @KafkaListener(topics = "publicaciones.registradas")
    public void publicacionRegistrada(String mensaje) {
        conTraza(mensaje, () -> proyector.alRegistrarsePublicacion(mensaje));
    }

    @KafkaListener(topics = "investigadores.en-eliminacion")
    public void investigadorEnEliminacion(String mensaje) {
        conTraza(mensaje, () -> proyector.alIniciarseEliminacion(mensaje));
    }

    @KafkaListener(topics = "investigadores.eliminados")
    public void investigadorEliminado(String mensaje) {
        conTraza(mensaje, () -> proyector.alEliminarseInvestigador(mensaje));
    }

    private void conTraza(String mensaje, Runnable proyeccion) {
        try (MDC.MDCCloseable ignorado = MDC.putCloseable("correlationId", correlationIdDe(mensaje))) {
            proyeccion.run();
        }
    }

    /** Un mensaje ilegible no tiene id; el proyector lanzará el error y el mensaje irá a la DLT. */
    private String correlationIdDe(String mensaje) {
        try {
            JsonNode evento = objectMapper.readTree(mensaje);
            return evento.has("correlationId") ? evento.get("correlationId").asString() : "";
        } catch (JacksonException ilegible) {
            return "";
        }
    }
}
