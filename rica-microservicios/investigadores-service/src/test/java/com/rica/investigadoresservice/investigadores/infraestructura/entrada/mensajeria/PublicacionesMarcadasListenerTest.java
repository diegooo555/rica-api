package com.rica.investigadoresservice.investigadores.infraestructura.entrada.mensajeria;

import com.rica.investigadoresservice.investigadores.aplicacion.InvestigadorUseCase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PublicacionesMarcadasListenerTest {

    @Test
    void laConfirmacionDePublicacionesCierraLaSaga() {
        InvestigadorUseCase useCase = mock(InvestigadorUseCase.class);
        PublicacionesMarcadasListener listener = new PublicacionesMarcadasListener(useCase, JsonMapper.builder().build());

        listener.alMarcarsePublicaciones("{\"correoInstitucional\":\"ana.torres@uptc.edu.co\",\"cantidad\":2}");

        verify(useCase).confirmarEliminacion("ana.torres@uptc.edu.co");
    }
}
