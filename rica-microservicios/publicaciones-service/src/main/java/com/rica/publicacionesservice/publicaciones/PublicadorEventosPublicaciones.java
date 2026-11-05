package com.rica.publicacionesservice.publicaciones;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Lado de escritura de CQRS: cada vez que se registra una publicación (comando),
 * se anuncia el hecho para que otros servicios puedan construir sus propias vistas.
 */
@Component
public class PublicadorEventosPublicaciones {

    static final String TOPICO_PUBLICACION_REGISTRADA = "publicaciones.registradas";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PublicadorEventosPublicaciones(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicacionRegistrada(Publicacion publicacion) {
        String contenido = objectMapper.writeValueAsString(Map.of(
                "id", publicacion.getId(),
                "investigadorCorreo", publicacion.getInvestigadorCorreo(),
                "titulo", publicacion.getTitulo(),
                "tipo", publicacion.getTipo(),
                "anio", publicacion.getAnio()));
        // La clave es el correo: todos los eventos de un investigador caen en la misma partición, en orden.
        kafkaTemplate.send(TOPICO_PUBLICACION_REGISTRADA, publicacion.getInvestigadorCorreo(), contenido);
    }
}
