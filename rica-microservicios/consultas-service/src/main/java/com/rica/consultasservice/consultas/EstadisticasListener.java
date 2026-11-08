package com.rica.consultasservice.consultas;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Segundo consumidor del MISMO tópico, con su propio grupo: cada grupo recibe todos los
 * mensajes por separado. Cuenta las altas de investigadores, ignorando las repetidas.
 */
@Component
public class EstadisticasListener {

    private static final Logger log = LoggerFactory.getLogger(EstadisticasListener.class);

    private final RegistroEventosProcesados eventosProcesados;
    private final ContadorAltas contador;
    private final ObjectMapper objectMapper;

    public EstadisticasListener(RegistroEventosProcesados eventosProcesados,
                                ContadorAltas contador,
                                ObjectMapper objectMapper) {
        this.eventosProcesados = eventosProcesados;
        this.contador = contador;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "investigadores.registrados", groupId = "consultas-estadisticas")
    public void alRegistrarseInvestigador(ConsumerRecord<String, String> registro) {
        if (!esAltaValida(registro.value())) {
            log.warn("Estadísticas: mensaje no válido en {}-{}@{}, no se cuenta",
                    registro.topic(), registro.partition(), registro.offset());
            return;
        }
        // Identidad del evento = su posición en el log de Kafka (tópico-partición-offset)
        String idEvento = registro.topic() + "-" + registro.partition() + "-" + registro.offset();
        if (!eventosProcesados.primeraVez(idEvento)) {
            log.info("Estadísticas: evento {} ya procesado, se ignora", idEvento);
            return;
        }
        contador.contarAlta();
        log.info("Estadísticas: alta contada ({}). Total: {}", idEvento, contador.total());
    }

    private boolean esAltaValida(String mensaje) {
        try {
            objectMapper.readTree(mensaje).get("correoInstitucional").asString();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
