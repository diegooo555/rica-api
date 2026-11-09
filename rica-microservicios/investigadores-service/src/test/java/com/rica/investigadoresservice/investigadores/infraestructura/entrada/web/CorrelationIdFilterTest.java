package com.rica.investigadoresservice.investigadores.infraestructura.entrada.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    @Test
    void respetaElIdQueEnviaElClienteYLoDevuelveEnLaRespuesta() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/investigadores");
        request.addHeader(CorrelationIdFilter.CABECERA, "demo-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> visto = new AtomicReference<>();

        filtro.doFilter(request, response, (req, res) -> visto.set(MDC.get(CorrelationIdFilter.CLAVE_MDC)));

        assertThat(visto.get()).isEqualTo("demo-001");
        assertThat(response.getHeader(CorrelationIdFilter.CABECERA)).isEqualTo("demo-001");
    }

    @Test
    void generaUnIdSiNoVieneYLimpiaElMdcAlTerminar() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/api/investigadores"), response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.CABECERA)).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.CLAVE_MDC)).isNull();
    }
}
