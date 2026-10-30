package com.rica.publicacionesservice.publicaciones;

import org.springframework.stereotype.Component;

@Component
public class VerificadorInvestigadorPendiente implements VerificadorInvestigador {

    // TODO Taller Lección 6: reemplazar por un adaptador gRPC real
    // hacia investigadores-service, protegido con Circuit Breaker.
    // Por ahora, todo correo se acepta — publicaciones-service no tiene
    // forma de confirmar un investigador sin cruzar la red.
    @Override
    public boolean existe(String correoInstitucional) {
        return true;
    }
}
