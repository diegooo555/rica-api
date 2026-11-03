package com.rica.investigadoresservice.investigadores.infraestructura.salida.mensajeria;

import com.rica.investigadoresservice.investigadores.aplicacion.PublicadorEventos;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Adaptador: en vez de hablar con Kafka, escribe el evento en la tabla outbox.
 * Se ejecuta dentro de la misma transacción que guarda al investigador, así
 * ambos cambios se confirman juntos o ninguno se confirma.
 */
@Component
public class OutboxPublicadorEventos implements PublicadorEventos {

    private final OutboxEventRepository repositorio;
    private final ObjectMapper objectMapper;

    public OutboxPublicadorEventos(OutboxEventRepository repositorio, ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publicar(String topico, String clave, Map<String, Object> contenido) {
        String payload = objectMapper.writeValueAsString(contenido);
        repositorio.save(new OutboxEvent(topico, clave, payload));
    }
}
