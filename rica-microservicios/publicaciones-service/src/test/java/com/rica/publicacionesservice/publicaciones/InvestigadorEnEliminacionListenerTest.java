package com.rica.publicacionesservice.publicaciones;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvestigadorEnEliminacionListenerTest {

    private final PublicacionRepository repositorio = mock(PublicacionRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final InvestigadorEnEliminacionListener listener =
            new InvestigadorEnEliminacionListener(repositorio, kafkaTemplate, JsonMapper.builder().build());

    @Test
    void marcaLasPublicacionesComoHuerfanasYConfirma() {
        Publicacion p1 = new Publicacion("p1", "ana.torres@uptc.edu.co", "Uno", "articulo", 2026);
        Publicacion p2 = new Publicacion("p2", "ana.torres@uptc.edu.co", "Dos", "libro", 2025);
        when(repositorio.findByInvestigadorCorreo("ana.torres@uptc.edu.co")).thenReturn(List.of(p1, p2));

        listener.alIniciarseEliminacion("{\"id\":1,\"correoInstitucional\":\"ana.torres@uptc.edu.co\"}");

        assertThat(p1.isHuerfana()).isTrue();
        assertThat(p2.isHuerfana()).isTrue();
        verify(repositorio).saveAll(List.of(p1, p2));
        verify(kafkaTemplate).send(eq("publicaciones.marcadas-huerfanas"), eq("ana.torres@uptc.edu.co"),
                contains("\"cantidad\":2"));
    }

    @Test
    void laConfirmacionConservaElIdDeCorrelacionDelEvento() {
        when(repositorio.findByInvestigadorCorreo("ana.torres@uptc.edu.co")).thenReturn(List.of());

        listener.alIniciarseEliminacion(
                "{\"correoInstitucional\":\"ana.torres@uptc.edu.co\",\"correlationId\":\"demo-001\"}");

        verify(kafkaTemplate).send(eq("publicaciones.marcadas-huerfanas"), eq("ana.torres@uptc.edu.co"),
                contains("\"correlationId\":\"demo-001\""));
    }

    @Test
    void confirmaAunqueElInvestigadorNoTengaPublicaciones() {
        when(repositorio.findByInvestigadorCorreo("luis.mora@uptc.edu.co")).thenReturn(List.of());

        listener.alIniciarseEliminacion("{\"id\":2,\"correoInstitucional\":\"luis.mora@uptc.edu.co\"}");

        verify(kafkaTemplate).send(eq("publicaciones.marcadas-huerfanas"), eq("luis.mora@uptc.edu.co"),
                contains("\"cantidad\":0"));
    }
}
