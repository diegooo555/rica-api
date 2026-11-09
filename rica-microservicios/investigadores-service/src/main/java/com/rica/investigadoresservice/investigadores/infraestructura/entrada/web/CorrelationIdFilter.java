package com.rica.investigadoresservice.investigadores.infraestructura.entrada.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Da a cada petición un identificador de correlación: lo toma de la cabecera
 * X-Correlation-Id si el cliente lo envía, o genera uno nuevo. Queda en el MDC
 * (y por tanto en cada línea de log) y se devuelve en la respuesta.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Correlation-Id";
    public static final String CLAVE_MDC = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String id = request.getHeader(CABECERA);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(CLAVE_MDC, id);
        response.setHeader(CABECERA, id);
        try {
            chain.doFilter(request, response);
        } finally {
            // Los hilos se reutilizan: un id "pegado" contaminaría la petición siguiente.
            MDC.remove(CLAVE_MDC);
        }
    }
}
