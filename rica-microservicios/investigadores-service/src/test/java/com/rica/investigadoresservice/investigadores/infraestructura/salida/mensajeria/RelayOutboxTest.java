package com.rica.investigadoresservice.investigadores.infraestructura.salida.mensajeria;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelayOutboxTest {

    private final OutboxEventRepository repositorio = mock(OutboxEventRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final RelayOutbox relay = new RelayOutbox(repositorio, kafkaTemplate);

    @Test
    void publicaLosPendientesYLosMarcaComoPublicados() {
        OutboxEvent evento = new OutboxEvent("investigadores.registrados", "ana.torres@uptc.edu.co", "{}");
        when(repositorio.findTop100ByPublicadoEnIsNullOrderByIdAsc()).thenReturn(List.of(evento));
        when(kafkaTemplate.send("investigadores.registrados", "ana.torres@uptc.edu.co", "{}"))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        relay.publicarPendientes();

        assertThat(evento.getPublicadoEn()).isNotNull();
        verify(repositorio).save(evento);
    }

    @Test
    void siKafkaFallaElEventoQuedaPendienteYNoSeSaltaAlSiguiente() {
        OutboxEvent primero = new OutboxEvent("investigadores.registrados", "a@uptc.edu.co", "{}");
        OutboxEvent segundo = new OutboxEvent("investigadores.registrados", "b@uptc.edu.co", "{}");
        when(repositorio.findTop100ByPublicadoEnIsNullOrderByIdAsc()).thenReturn(List.of(primero, segundo));
        when(kafkaTemplate.send("investigadores.registrados", "a@uptc.edu.co", "{}"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

        relay.publicarPendientes();

        assertThat(primero.getPublicadoEn()).isNull();
        verify(kafkaTemplate, never()).send("investigadores.registrados", "b@uptc.edu.co", "{}");
        verify(repositorio, never()).save(any());
    }
}
