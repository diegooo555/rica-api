package com.rica.consultasservice.consultas;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class ProyeccionListenerTest {

    private final ProyectorFichas proyector = mock(ProyectorFichas.class);
    private final ProyeccionListener listener = new ProyeccionListener(proyector, JsonMapper.builder().build());

    @Test
    void proyectaConElIdDeCorrelacionDelEventoYLuegoLimpiaElMdc() {
        AtomicReference<String> visto = new AtomicReference<>();
        doAnswer(i -> { visto.set(MDC.get("correlationId")); return null; })
                .when(proyector).alIniciarseEliminacion(anyString());

        listener.investigadorEnEliminacion("{\"correoInstitucional\":\"ana.torres@uptc.edu.co\",\"correlationId\":\"demo-001\"}");

        assertThat(visto.get()).isEqualTo("demo-001");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void unMensajeIlegibleIgualLlegaAlProyectorParaQueFalleYVayaALaDlt() {
        AtomicReference<String> recibido = new AtomicReference<>();
        doAnswer(i -> { recibido.set(i.getArgument(0)); return null; })
                .when(proyector).alRegistrarseInvestigador(anyString());

        listener.investigadorRegistrado("esto no es json");

        assertThat(recibido.get()).isEqualTo("esto no es json");
    }
}
