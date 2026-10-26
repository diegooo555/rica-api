package com.rica.ricaapi.investigadores;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rica.ricaapi.compartido.RecursoNoEncontradoException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class InvestigadorServiceTest {

    @Mock
    private InvestigadorRepository investigadorRepository;

    @Mock
    private InvestigadorFactory investigadorFactory;

    @InjectMocks
    private InvestigadorService investigadorService;

    @Test
    void buscarPorIdDevuelveElInvestigadorCuandoExiste() {
        Investigador investigador = new Investigador(1L, "Ana Torres",
                new CorreoInstitucional("ana.torres@uptc.edu.co"), "GIT-UPTC");
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
    void registrarDelegaLaCreacionEnLaFactoryYGuardaElResultado() {
        Investigador creado = new Investigador(null, "Carlos Ruiz",
                new CorreoInstitucional("carlos.ruiz@uptc.edu.co"), "GIT-UPTC");
        when(investigadorFactory.crear("Carlos Ruiz", "carlos.ruiz@uptc.edu.co", "GIT-UPTC")).thenReturn(creado);
        when(investigadorRepository.save(creado)).thenReturn(creado);

        Investigador resultado = investigadorService.registrar("Carlos Ruiz", "carlos.ruiz@uptc.edu.co", "GIT-UPTC");

        assertThat(resultado).isSameAs(creado);
        verify(investigadorRepository).save(creado);
    }

    @Test
    void registrarNoGuardaNadaSiLaFactoryRechazaElCorreo() {
        when(investigadorFactory.crear("Carlos Ruiz", "carlos.ruiz@uptc.edu.co", "GIT-UPTC"))
                .thenThrow(new CorreoDuplicadoException("duplicado"));

        assertThatThrownBy(() -> investigadorService.registrar("Carlos Ruiz", "carlos.ruiz@uptc.edu.co", "GIT-UPTC"))
                .isInstanceOf(CorreoDuplicadoException.class);

        verify(investigadorRepository, never()).save(any());
    }

}
