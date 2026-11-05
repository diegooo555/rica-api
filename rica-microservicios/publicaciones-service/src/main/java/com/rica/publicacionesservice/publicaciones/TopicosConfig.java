package com.rica.publicacionesservice.publicaciones;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Tópicos que publica y consume publicaciones-service. Declarar uno que ya existe no hace nada. */
@Configuration
public class TopicosConfig {

    @Bean
    public NewTopic investigadoresEnEliminacion() {
        return TopicBuilder.name("investigadores.en-eliminacion").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic publicacionesRegistradas() {
        return TopicBuilder.name(PublicadorEventosPublicaciones.TOPICO_PUBLICACION_REGISTRADA).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic publicacionesMarcadasHuerfanas() {
        return TopicBuilder.name(InvestigadorEnEliminacionListener.TOPICO_CONFIRMACION).partitions(1).replicas(1).build();
    }
}
