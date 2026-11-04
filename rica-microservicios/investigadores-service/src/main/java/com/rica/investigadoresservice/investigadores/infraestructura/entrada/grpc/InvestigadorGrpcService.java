package com.rica.investigadoresservice.investigadores.infraestructura.entrada.grpc;

import com.rica.investigadoresservice.compartido.RecursoNoEncontradoException;
import com.rica.investigadoresservice.grpc.CorreoRequest;
import com.rica.investigadoresservice.grpc.InvestigadorGrpcGrpc;
import com.rica.investigadoresservice.grpc.InvestigadorRequest;
import com.rica.investigadoresservice.grpc.InvestigadorResponse;
import com.rica.investigadoresservice.investigadores.aplicacion.InvestigadorUseCase;
import com.rica.investigadoresservice.investigadores.dominio.Investigador;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.Optional;

/**
 * Adaptador primario gRPC: expone al resto de servicios la pregunta "¿existe este investigador?".
 * Igual que el controlador REST, solo traduce entre el protocolo y el puerto primario InvestigadorUseCase.
 */
@GrpcService
public class InvestigadorGrpcService extends InvestigadorGrpcGrpc.InvestigadorGrpcImplBase {

    private final InvestigadorUseCase investigadorUseCase;

    public InvestigadorGrpcService(InvestigadorUseCase investigadorUseCase) {
        this.investigadorUseCase = investigadorUseCase;
    }

    @Override
    public void existeInvestigador(InvestigadorRequest request, StreamObserver<InvestigadorResponse> responseObserver) {
        Optional<Investigador> encontrado;
        try {
            encontrado = Optional.of(investigadorUseCase.buscarPorId(request.getId()));
        } catch (RecursoNoEncontradoException noExiste) {
            encontrado = Optional.empty();
        }
        responder(encontrado, responseObserver);
    }

    @Override
    public void existeInvestigadorPorCorreo(CorreoRequest request, StreamObserver<InvestigadorResponse> responseObserver) {
        // Bloqueo semántico: un investigador EN_ELIMINACION ya no cuenta como existente para registrar publicaciones.
        responder(investigadorUseCase.buscarPorCorreo(request.getCorreoInstitucional())
                .filter(Investigador::aceptaPublicaciones), responseObserver);
    }

    private void responder(Optional<Investigador> encontrado, StreamObserver<InvestigadorResponse> responseObserver) {
        InvestigadorResponse respuesta = InvestigadorResponse.newBuilder()
                .setExiste(encontrado.isPresent())
                .setNombreCompleto(encontrado.map(Investigador::getNombreCompleto).orElse(""))
                .build();
        responseObserver.onNext(respuesta);
        responseObserver.onCompleted();
    }
}
