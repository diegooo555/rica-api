package com.rica.investigadoresservice.investigadores.infraestructura.salida.mensajeria;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxPublicadorEventosTest {

    private final OutboxEventRepository repositorio = mock(OutboxEventRepository.class);
    private final OutboxPublicadorEventos publicador = new OutboxPublicadorEventos(repositorio, JsonMapper.builder().build());

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    private OutboxEvent guardado() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repositorio).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void copiaElIdDeCorrelacionDelMdcDentroDelEvento() {
        MDC.put("correlationId", "demo-001");

        publicador.publicar("investigadores.en-eliminacion", "ana.torres@uptc.edu.co",
                Map.of("correoInstitucional", "ana.torres@uptc.edu.co"));

        assertThat(guardado().getPayload()).contains("\"correlationId\":\"demo-001\"");
    }

    @Test
    void sinIdDeCorrelacionElEventoNoLlevaElCampo() {
        publicador.publicar("investigadores.registrados", "ana.torres@uptc.edu.co",
                Map.of("correoInstitucional", "ana.torres@uptc.edu.co"));

        assertThat(guardado().getPayload()).doesNotContain("correlationId");
    }
}
