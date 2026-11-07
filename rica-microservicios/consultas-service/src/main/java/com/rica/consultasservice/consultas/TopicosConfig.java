package com.rica.consultasservice.consultas;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * El lado de lectura declara los tópicos que consume, para poder arrancar
 * aunque nadie haya publicado todavía en ellos. Declarar un tópico que ya existe no hace nada.
 */
@Configuration
public class TopicosConfig {

    @Bean
    public NewTopic investigadoresRegistrados() {
        return TopicBuilder.name("investigadores.registrados").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic investigadoresEnEliminacion() {
        return TopicBuilder.name("investigadores.en-eliminacion").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic investigadoresEliminados() {
        return TopicBuilder.name("investigadores.eliminados").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic publicacionesRegistradas() {
        return TopicBuilder.name("publicaciones.registradas").partitions(1).replicas(1).build();
    }

    /** Donde van a parar los mensajes de investigadores.registrados que no se pudieron procesar. */
    @Bean
    public NewTopic investigadoresRegistradosDlt() {
        return TopicBuilder.name("investigadores.registrados.DLT").partitions(1).replicas(1).build();
    }
}
