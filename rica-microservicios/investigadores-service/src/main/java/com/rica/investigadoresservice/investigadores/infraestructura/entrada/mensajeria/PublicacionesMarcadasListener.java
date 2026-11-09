package com.rica.investigadoresservice.investigadores.infraestructura.entrada.mensajeria;

import com.rica.investigadoresservice.investigadores.aplicacion.InvestigadorUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Último paso de la Saga: publicaciones-service confirmó que ya marcó las
 * publicaciones como huérfanas, así que ahora sí se borra al investigador.
 */
@Component
public class PublicacionesMarcadasListener {

    private static final Logger log = LoggerFactory.getLogger(PublicacionesMarcadasListener.class);

    private final InvestigadorUseCase investigadorService;
    private final ObjectMapper objectMapper;

    public PublicacionesMarcadasListener(InvestigadorUseCase investigadorService, ObjectMapper objectMapper) {
        this.investigadorService = investigadorService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "publicaciones.marcadas-huerfanas")
    public void alMarcarsePublicaciones(String mensaje) {
        JsonNode evento = objectMapper.readTree(mensaje);
        String correo = evento.get("correoInstitucional").asString();
        // Se retoma el id de correlación que venía en el evento para seguir la misma traza
        String correlationId = evento.has("correlationId") ? evento.get("correlationId").asString() : "";
        try (MDC.MDCCloseable ignorado = MDC.putCloseable("correlationId", correlationId)) {
            log.info("Saga: publicaciones confirmó para {}. Eliminando investigador.", correo);
            investigadorService.confirmarEliminacion(correo);
        }
    }
}
