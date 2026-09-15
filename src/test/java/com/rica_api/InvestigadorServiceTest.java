package com.rica_api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rica.ricaapi.compartido.RecursoNoEncontradoException;
import com.rica.ricaapi.investigadores.CorreoDuplicadoException;
import com.rica.ricaapi.investigadores.Investigador;
import com.rica.ricaapi.investigadores.InvestigadorRepository;
import com.rica.ricaapi.investigadores.InvestigadorService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestigadorServiceTest {

    @Mock
    private InvestigadorRepository investigadorRepository;

    @InjectMocks
    private InvestigadorService investigadorService;

    @Test
    void buscarPorIdDevuelveElInvestigadorCuandoExiste() {
        Investigador investigador = new Investigador(1L, "Ana Torres", "ana.torres@uptc.edu.co", "GIT-UPTC");
        when(investigadorRepository.findById(1L)).thenReturn(Optional.of(investigador));

        Investigador resultado = investigadorService.buscarPorId(1L);

        assertThat(resultado.getNombreCompleto()).isEqualTo("Ana Torres");
    }

    @Test
    void buscarPorIdLanzaExcepcionCuandoNoExiste() {
        when(investigadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investigadorService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void registrarRechazaCorreoInstitucionalDuplicado() {
        Investigador nuevo = new Investigador(null, "Carlos Ruiz", "carlos.ruiz@uptc.edu.co", "GIT-UPTC");
        when(investigadorRepository.existsByCorreoInstitucional("carlos.ruiz@uptc.edu.co")).thenReturn(true);

        assertThatThrownBy(() -> investigadorService.registrar(nuevo))
                .isInstanceOf(CorreoDuplicadoException.class);

        verify(investigadorRepository).existsByCorreoInstitucional("carlos.ruiz@uptc.edu.co");
    }

}
