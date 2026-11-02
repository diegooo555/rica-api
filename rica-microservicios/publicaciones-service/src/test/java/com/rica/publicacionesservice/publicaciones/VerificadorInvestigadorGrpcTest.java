package com.rica.publicacionesservice.publicaciones;

import com.rica.publicacionesservice.compartido.ServicioNoDisponibleException;
import com.rica.publicacionesservice.grpc.CorreoRequest;
import com.rica.publicacionesservice.grpc.InvestigadorGrpcGrpc.InvestigadorGrpcBlockingStub;
import com.rica.publicacionesservice.grpc.InvestigadorResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificadorInvestigadorGrpcTest {

    private InvestigadorGrpcBlockingStub stub;
    private VerificadorInvestigadorGrpc verificador;

    @BeforeEach
    void preparar() {
        stub = mock(InvestigadorGrpcBlockingStub.class);
        when(stub.withDeadlineAfter(anyLong(), any())).thenReturn(stub);
        Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
                CircuitBreakerRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(), null);
        new ComunicacionConfig().circuitoInvestigadores().customize(factory);
        verificador = new VerificadorInvestigadorGrpc(stub, factory);
    }

    @Test
    void devuelveLaRespuestaDeInvestigadoresService() {
        when(stub.existeInvestigadorPorCorreo(any(CorreoRequest.class)))
                .thenReturn(InvestigadorResponse.newBuilder().setExiste(true).build());

        assertThat(verificador.existe("ana.torres@uptc.edu.co")).isTrue();
    }

    @Test
    void fallaConServicioNoDisponibleSiInvestigadoresServiceNoResponde() {
        when(stub.existeInvestigadorPorCorreo(any(CorreoRequest.class)))
                .thenThrow(Status.UNAVAILABLE.asRuntimeException());

        assertThatThrownBy(() -> verificador.existe("ana.torres@uptc.edu.co"))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void conElCircuitoAbiertoNiSiquieraIntentaLaLlamada() {
        when(stub.existeInvestigadorPorCorreo(any(CorreoRequest.class)))
                .thenThrow(Status.UNAVAILABLE.asRuntimeException());
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> verificador.existe("ana.torres@uptc.edu.co"))
                    .isInstanceOf(ServicioNoDisponibleException.class);
        }
        // Circuito abierto: aunque el servidor "volviera", la llamada se corta sin tocar la red.
        when(stub.existeInvestigadorPorCorreo(any(CorreoRequest.class)))
                .thenReturn(InvestigadorResponse.newBuilder().setExiste(true).build());

        assertThatThrownBy(() -> verificador.existe("ana.torres@uptc.edu.co"))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
