package com.rica.publicacionesservice.publicaciones;

import com.rica.publicacionesservice.compartido.ServicioNoDisponibleException;
import com.rica.publicacionesservice.grpc.CorreoRequest;
import com.rica.publicacionesservice.grpc.InvestigadorGrpcGrpc.InvestigadorGrpcBlockingStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Adaptador secundario real del puerto VerificadorInvestigador: pregunta por gRPC a
 * investigadores-service, protegido con un Circuit Breaker (Resilience4j).
 *
 * Si investigadores-service no responde, el circuito evita esperar el timeout completo en
 * cada petición y la operación falla de inmediato con 503. Se prefiere fallar cerrado
 * (no registrar) antes que aceptar publicaciones de investigadores que no se pudieron verificar.
 */
@Component
public class VerificadorInvestigadorGrpc implements VerificadorInvestigador {

    static final String CIRCUITO = "investigadores";

    private static final Logger log = LoggerFactory.getLogger(VerificadorInvestigadorGrpc.class);
    private static final long PLAZO_MAXIMO_MS = 2_000;

    private final InvestigadorGrpcBlockingStub stub;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public VerificadorInvestigadorGrpc(InvestigadorGrpcBlockingStub stub, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.stub = stub;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @Override
    public boolean existe(String correoInstitucional) {
        CorreoRequest request = CorreoRequest.newBuilder().setCorreoInstitucional(correoInstitucional).build();
        return circuitBreakerFactory.create(CIRCUITO).run(
                () -> stub.withDeadlineAfter(PLAZO_MAXIMO_MS, TimeUnit.MILLISECONDS)
                        .existeInvestigadorPorCorreo(request)
                        .getExiste(),
                error -> {
                    log.warn("No se pudo verificar al investigador {}: {}", correoInstitucional, error.toString());
                    throw new ServicioNoDisponibleException(
                            "investigadores-service no está disponible; intente de nuevo en unos segundos");
                });
    }
}
