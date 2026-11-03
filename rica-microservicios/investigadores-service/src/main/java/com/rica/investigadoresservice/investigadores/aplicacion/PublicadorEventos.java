package com.rica.investigadoresservice.investigadores.aplicacion;

import java.util.Map;

/**
 * Puerto secundario: anunciar hechos del dominio a otros servicios.
 * El núcleo no sabe si detrás hay Kafka, RabbitMQ o una tabla outbox.
 */
public interface PublicadorEventos {

    void publicar(String topico, String clave, Map<String, Object> contenido);
}
