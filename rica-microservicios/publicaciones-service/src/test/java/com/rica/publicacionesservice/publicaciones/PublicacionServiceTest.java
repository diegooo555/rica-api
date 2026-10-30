package com.rica.publicacionesservice.publicaciones;

import com.rica.publicacionesservice.compartido.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicacionServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private VerificadorInvestigador verificadorInvestigador;

    @Mock
    private LimitePublicacionesAnualesService limitePublicacionesAnualesService;

    @InjectMocks
    private PublicacionService publicacionService;

    private Publicacion nuevaPublicacion() {
        return new Publicacion(null, "ana.torres@uptc.edu.co", "Arquitecturas evolutivas", "articulo", 2026);
    }

    @Test
    void registrarGuardaCuandoElInvestigadorExisteYNoSuperaElLimite() {
        Publicacion entrada = nuevaPublicacion();
        when(verificadorInvestigador.existe("ana.torres@uptc.edu.co")).thenReturn(true);
        when(limitePublicacionesAnualesService.puedeRegistrar("ana.torres@uptc.edu.co", 2026)).thenReturn(true);
        when(publicacionRepository.save(entrada)).thenReturn(entrada);

        assertThat(publicacionService.registrar(entrada)).isSameAs(entrada);
    }

    @Test
    void registrarRechazaUnInvestigadorInexistente() {
        when(verificadorInvestigador.existe("nadie@uptc.edu.co")).thenReturn(false);
        Publicacion entrada = new Publicacion(null, "nadie@uptc.edu.co", "X", "libro", 2026);

        assertThatThrownBy(() -> publicacionService.registrar(entrada))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void registrarRechazaCuandoSeAlcanzoElLimiteAnual() {
        Publicacion entrada = nuevaPublicacion();
        when(verificadorInvestigador.existe("ana.torres@uptc.edu.co")).thenReturn(true);
        when(limitePublicacionesAnualesService.puedeRegistrar("ana.torres@uptc.edu.co", 2026)).thenReturn(false);

        assertThatThrownBy(() -> publicacionService.registrar(entrada))
                .isInstanceOf(LimiteAnualExcedidoException.class);
        verify(publicacionRepository, never()).save(any());
    }
}
