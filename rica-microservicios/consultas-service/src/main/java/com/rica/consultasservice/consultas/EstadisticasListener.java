package com.rica.consultasservice.consultas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Segundo consumidor del MISMO tópico, con su propio grupo: cada grupo recibe todos los
 * mensajes por separado. Versión ingenua: cuenta cada mensaje que recibe.
 */
@Component
public class EstadisticasListener {

    private static final Logger log = LoggerFactory.getLogger(EstadisticasListener.class);

    private final ContadorAltas contador;
    private final ObjectMapper objectMapper;

    public EstadisticasListener(ContadorAltas contador, ObjectMapper objectMapper) {
        this.contador = contador;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "investigadores.registrados", groupId = "consultas-estadisticas")
    public void alRegistrarseInvestigador(String mensaje) {
        // Solo cuenta mensajes que parezcan un evento de registro válido
        try {
            objectMapper.readTree(mensaje).get("correoInstitucional").asString();
        } catch (RuntimeException e) {
            log.warn("Estadísticas: mensaje no válido, no se cuenta");
            return;
        }
        contador.contarAlta();
        log.info("Estadísticas: alta contada. Total: {}", contador.total());
    }
}
