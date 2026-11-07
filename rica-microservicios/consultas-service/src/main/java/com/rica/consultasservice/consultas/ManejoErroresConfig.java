package com.rica.consultasservice.consultas;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import tools.jackson.core.JacksonException;

/**
 * Qué hacer cuando procesar un mensaje falla:
 * - errores que no se arreglan reintentando (JSON roto, campo faltante) van directo a la DLT;
 * - el resto se reintenta con espera creciente (1 s, 2 s, 4 s) y, si sigue fallando, también va a la DLT.
 * Así un mensaje venenoso no bloquea la partición ni se pierde en silencio.
 */
@Configuration
public class ManejoErroresConfig {

    @Bean
    public DefaultErrorHandler manejadorDeErrores(KafkaTemplate<String, String> kafkaTemplate) {
        // Publica el mensaje fallido en "<topico-original>.DLT", con cabeceras que explican el fallo.
        // Se fija el nombre a mano: desde Spring Kafka 4 el sufijo por defecto es "-dlt", no ".DLT".
        DeadLetterPublishingRecoverer recuperador = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (registro, error) -> new TopicPartition(registro.topic() + ".DLT", registro.partition()));

        ExponentialBackOffWithMaxRetries espera = new ExponentialBackOffWithMaxRetries(3);
        espera.setInitialInterval(1_000);
        espera.setMultiplier(2.0);
        espera.setMaxInterval(8_000);

        DefaultErrorHandler manejador = new DefaultErrorHandler(recuperador, espera);
        manejador.addNotRetryableExceptions(JacksonException.class, NullPointerException.class);
        return manejador;
    }
}
