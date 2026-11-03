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

    @Bean
    public NewTopic investigadoresRegistrados() {
        return TopicBuilder.name(INVESTIGADORES_REGISTRADOS).partitions(1).replicas(1).build();
    }
}
