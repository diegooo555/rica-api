package com.rica.ricaapi.investigadores;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestigadorFactoryTest {

    @Mock
    private InvestigadorRepository investigadorRepository;

    @InjectMocks
    private InvestigadorFactory investigadorFactory;

    @Test
    void creaUnInvestigadorNuevoSinIdCuandoElCorreoEstaLibre() {
        when(investigadorRepository.existsByCorreoInstitucional_Valor("ana.torres@uptc.edu.co")).thenReturn(false);

        Investigador investigador = investigadorFactory.crear("Ana Torres", "ana.torres@uptc.edu.co", "GIT-UPTC");

        assertThat(investigador.getId()).isNull();
        assertThat(investigador.getCorreoInstitucional().valor()).isEqualTo("ana.torres@uptc.edu.co");
    }

    @Test
    void rechazaUnCorreoInstitucionalDuplicado() {
        when(investigadorRepository.existsByCorreoInstitucional_Valor("ana.torres@uptc.edu.co")).thenReturn(true);

        assertThatThrownBy(() -> investigadorFactory.crear("Ana Torres", "ana.torres@uptc.edu.co", "GIT-UPTC"))
                .isInstanceOf(CorreoDuplicadoException.class);
    }

    @Test
    void rechazaUnCorreoFueraDelDominioInstitucional() {
        assertThatThrownBy(() -> investigadorFactory.crear("Ana Torres", "ana@gmail.com", "GIT-UPTC"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@uptc.edu.co");
    }

}
