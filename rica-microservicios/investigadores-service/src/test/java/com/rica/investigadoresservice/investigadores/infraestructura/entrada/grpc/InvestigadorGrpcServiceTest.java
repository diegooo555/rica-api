package com.rica.investigadoresservice.investigadores.infraestructura.entrada.grpc;

import com.rica.investigadoresservice.compartido.RecursoNoEncontradoException;
import com.rica.investigadoresservice.grpc.CorreoRequest;
import com.rica.investigadoresservice.grpc.InvestigadorRequest;
import com.rica.investigadoresservice.grpc.InvestigadorResponse;
import com.rica.investigadoresservice.investigadores.aplicacion.InvestigadorUseCase;
import com.rica.investigadoresservice.investigadores.dominio.CorreoInstitucional;
import com.rica.investigadoresservice.investigadores.dominio.Investigador;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvestigadorGrpcServiceTest {

    private final InvestigadorUseCase useCase = mock(InvestigadorUseCase.class);
    private final InvestigadorGrpcService servicio = new InvestigadorGrpcService(useCase);

    /** Observador mínimo que guarda lo que el servicio responde. */
    private static class Captura implements StreamObserver<InvestigadorResponse> {
        final List<InvestigadorResponse> respuestas = new ArrayList<>();
        boolean completado;

        @Override public void onNext(InvestigadorResponse value) { respuestas.add(value); }
        @Override public void onError(Throwable t) { throw new AssertionError(t); }
        @Override public void onCompleted() { completado = true; }
    }

    @Test
    void respondeQueExisteConSuNombreCuandoElCorreoEstaRegistrado() {
        Investigador ana = new Investigador(1L, "Ana Torres", new CorreoInstitucional("ana.torres@uptc.edu.co"), "GIT");
        when(useCase.buscarPorCorreo("ana.torres@uptc.edu.co")).thenReturn(Optional.of(ana));
        Captura captura = new Captura();

        servicio.existeInvestigadorPorCorreo(
                CorreoRequest.newBuilder().setCorreoInstitucional("ana.torres@uptc.edu.co").build(), captura);

        assertThat(captura.completado).isTrue();
        assertThat(captura.respuestas.getFirst().getExiste()).isTrue();
        assertThat(captura.respuestas.getFirst().getNombreCompleto()).isEqualTo("Ana Torres");
    }

    @Test
    void respondeQueNoExisteCuandoElIdNoEstaRegistrado() {
        when(useCase.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));
        Captura captura = new Captura();

        servicio.existeInvestigador(InvestigadorRequest.newBuilder().setId(99L).build(), captura);

        assertThat(captura.respuestas.getFirst().getExiste()).isFalse();
        assertThat(captura.respuestas.getFirst().getNombreCompleto()).isEmpty();
    }

    @Test
    void unInvestigadorEnEliminacionYaNoAceptaPublicaciones() {
        Investigador ana = new Investigador(1L, "Ana Torres", new CorreoInstitucional("ana.torres@uptc.edu.co"), "GIT");
        ana.marcarEnEliminacion();
        when(useCase.buscarPorCorreo("ana.torres@uptc.edu.co")).thenReturn(Optional.of(ana));
        Captura captura = new Captura();

        servicio.existeInvestigadorPorCorreo(
                CorreoRequest.newBuilder().setCorreoInstitucional("ana.torres@uptc.edu.co").build(), captura);

        assertThat(captura.respuestas.getFirst().getExiste()).isFalse();
    }
}
