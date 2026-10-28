package com.rica.ricaapi.publicaciones;

import com.rica.ricaapi.investigadores.dominio.CorreoInstitucional;
import com.rica.ricaapi.investigadores.dominio.Investigador;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitePublicacionesAnualesServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @InjectMocks
    private LimitePublicacionesAnualesService limiteService;

    private final Investigador ana = new Investigador(1L, "Ana Torres",
            new CorreoInstitucional("ana.torres@uptc.edu.co"), "GIT-UPTC");

    private Publicacion publicacionDel(int anio) {
        Publicacion publicacion = new Publicacion();
        publicacion.setInvestigadorCorreo("ana.torres@uptc.edu.co");
        publicacion.setAnio(anio);
        return publicacion;
    }

    @Test
    void permiteRegistrarMientrasNoSeAlcanceElMaximoAnual() {
        when(publicacionRepository.countByInvestigadorCorreoAndAnio("ana.torres@uptc.edu.co", 2026)).thenReturn(4L);

        assertThat(limiteService.puedeRegistrar(ana, publicacionDel(2026))).isTrue();
    }

    @Test
    void rechazaCuandoYaSeAlcanzoElMaximoAnual() {
        when(publicacionRepository.countByInvestigadorCorreoAndAnio("ana.torres@uptc.edu.co", 2026)).thenReturn(5L);

        assertThat(limiteService.puedeRegistrar(ana, publicacionDel(2026))).isFalse();
    }

}
