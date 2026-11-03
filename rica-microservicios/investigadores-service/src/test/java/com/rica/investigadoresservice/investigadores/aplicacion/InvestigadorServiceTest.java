package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.investigadores.dominio.CorreoDuplicadoException;
import com.rica.investigadoresservice.investigadores.dominio.CorreoInstitucional;
import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rica.investigadoresservice.compartido.RecursoNoEncontradoException;

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
    private RepositorioInvestigadores repositorioInvestigadores;

    @Mock
    private InvestigadorFactory investigadorFactory;

    @Mock
    private ReportadorMinCiencias reportadorMinCiencias;

    @Mock
    private PublicadorEventos publicadorEventos;

    @InjectMocks
    private InvestigadorService investigadorService;

    @Test
    void buscarPorIdDevuelveElInvestigadorCuandoExiste() {
        Investigador investigador = new Investigador(1L, "Ana Torres",
                new CorreoInstitucional("ana.torres@uptc.edu.co"), "GIT-UPTC");
        when(repositorioInvestigadores.buscarPorId(1L)).thenReturn(Optional.of(investigador));

        Investigador resultado = investigadorService.buscarPorId(1L);

        assertThat(resultado.getNombreCompleto()).isEqualTo("Ana Torres");
    }

    @Test
    void buscarPorIdLanzaExcepcionCuandoNoExiste() {
        when(repositorioInvestigadores.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investigadorService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void registrarDelegaLaCreacionEnLaFactoryYGuardaElResultado() {
        Investigador creado = new Investigador(7L, "Carlos Ruiz",
                new CorreoInstitucional("carlos.ruiz@uptc.edu.co"), "GIT-UPTC");
        when(investigadorFactory.crear("Carlos Ruiz", "carlos.ruiz@uptc.edu.co", "GIT-UPTC")).thenReturn(creado);
        when(repositorioInvestigadores.guardar(creado)).thenReturn(creado);

        Investigador resultado = investigadorService.registrar("Carlos Ruiz", "carlos.ruiz@uptc.edu.co", "GIT-UPTC");

        assertThat(resultado).isSameAs(creado);
        verify(repositorioInvestigadores).guardar(creado);
        verify(reportadorMinCiencias).enviar(creado);
        verify(publicadorEventos).publicar("investigadores.registrados", "carlos.ruiz@uptc.edu.co", java.util.Map.of(
                "id", 7L,
                "correoInstitucional", "carlos.ruiz@uptc.edu.co",
                "nombreCompleto", "Carlos Ruiz",
                "grupoInvestigacion", "GIT-UPTC"));
    }

    @Test
    void registrarNoGuardaNadaSiLaFactoryRechazaElCorreo() {
        when(investigadorFactory.crear("Carlos Ruiz", "carlos.ruiz@uptc.edu.co", "GIT-UPTC"))
                .thenThrow(new CorreoDuplicadoException("duplicado"));

        assertThatThrownBy(() -> investigadorService.registrar("Carlos Ruiz", "carlos.ruiz@uptc.edu.co", "GIT-UPTC"))
                .isInstanceOf(CorreoDuplicadoException.class);

        verify(repositorioInvestigadores, never()).guardar(any());
        verify(publicadorEventos, never()).publicar(any(), any(), any());
    }

}
