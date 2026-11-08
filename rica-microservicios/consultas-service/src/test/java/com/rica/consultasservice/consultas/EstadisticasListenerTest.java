package com.rica.consultasservice.consultas;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EstadisticasListenerTest {

    private static final String ALTA_VALIDA =
            "{\"id\":1,\"correoInstitucional\":\"ana.torres@uptc.edu.co\",\"nombreCompleto\":\"Ana Torres\"}";

    private RegistroEventosProcesados eventosProcesados;
    private ContadorAltas contador;
    private EstadisticasListener listener;

    @BeforeEach
    void preparar() {
        eventosProcesados = mock(RegistroEventosProcesados.class);
        contador = mock(ContadorAltas.class);
        listener = new EstadisticasListener(eventosProcesados, contador, JsonMapper.builder().build());
    }

    private static ConsumerRecord<String, String> registro(long offset, String valor) {
        return new ConsumerRecord<>("investigadores.registrados", 0, offset, "ana.torres@uptc.edu.co", valor);
    }

    @Test
    void cuentaUnaAltaNueva() {
        when(eventosProcesados.primeraVez("investigadores.registrados-0-7")).thenReturn(true);

        listener.alRegistrarseInvestigador(registro(7, ALTA_VALIDA));

        verify(contador).contarAlta();
    }

    @Test
    void ignoraUnEventoYaProcesado() {
        when(eventosProcesados.primeraVez("investigadores.registrados-0-7")).thenReturn(false);

        listener.alRegistrarseInvestigador(registro(7, ALTA_VALIDA));

        verify(contador, never()).contarAlta();
    }

    @Test
    void noCuentaUnMensajeQueNoEsJsonValido() {
        listener.alRegistrarseInvestigador(registro(8, "esto no es json"));

        verify(contador, never()).contarAlta();
        verify(eventosProcesados, never()).primeraVez(anyString());
    }
}
