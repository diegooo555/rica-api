package com.rica.investigadoresservice.investigadores.infraestructura.salida.mensajeria;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tópicos que publica investigadores-service y activación del relay periódico del outbox. */
@Configuration
@EnableScheduling
public class MensajeriaConfig {

    public static final String INVESTIGADORES_REGISTRADOS = "investigadores.registrados";
    public static final String INVESTIGADORES_EN_ELIMINACION = "investigadores.en-eliminacion";
    public static final String PUBLICACIONES_MARCADAS_HUERFANAS = "publicaciones.marcadas-huerfanas";

    @Bean
    public NewTopic investigadoresRegistrados() {
        return TopicBuilder.name(INVESTIGADORES_REGISTRADOS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic investigadoresEnEliminacion() {
        return TopicBuilder.name(INVESTIGADORES_EN_ELIMINACION).partitions(1).replicas(1).build();
    }

    /** Lo publica publicaciones-service; se declara aquí también para poder escucharlo desde el arranque. */
    @Bean
    public NewTopic publicacionesMarcadasHuerfanas() {
        return TopicBuilder.name(PUBLICACIONES_MARCADAS_HUERFANAS).partitions(1).replicas(1).build();
    }
}
