package com.rica.consultasservice.consultas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class ProyectorFichasTest {

    private static final String CORREO = "ana.torres@uptc.edu.co";

    private final Map<String, FichaInvestigador> almacen = new HashMap<>();
    private ProyectorFichas proyector;

    @BeforeEach
    void preparar() {
        FichaRepository repositorio = Mockito.mock(FichaRepository.class);
        Mockito.when(repositorio.findById(anyString()))
                .thenAnswer(i -> Optional.ofNullable(almacen.get(i.<String>getArgument(0))));
        Mockito.when(repositorio.save(any(FichaInvestigador.class)))
                .thenAnswer(i -> {
                    FichaInvestigador ficha = i.getArgument(0);
                    almacen.put(ficha.getCorreo(), ficha);
                    return ficha;
                });
        Mockito.doAnswer(i -> almacen.remove(i.<String>getArgument(0)))
                .when(repositorio).deleteById(anyString());
        proyector = new ProyectorFichas(repositorio, JsonMapper.builder().build());
    }

    private static String eventoRegistro() {
        return "{\"id\":1,\"correoInstitucional\":\"" + CORREO + "\",\"nombreCompleto\":\"Ana Torres\"}";
    }

    private static String eventoPublicacion(String id) {
        return "{\"id\":\"" + id + "\",\"investigadorCorreo\":\"" + CORREO
                + "\",\"titulo\":\"Un titulo\",\"tipo\":\"articulo\",\"anio\":2026}";
    }

    @Test
    void elRegistroCreaLaFichaConSuNombre() {
        proyector.alRegistrarseInvestigador(eventoRegistro());

        assertThat(almacen.get(CORREO).getNombreCompleto()).isEqualTo("Ana Torres");
    }

    @Test
    void unaPublicacionQueLlegaAntesDelRegistroCreaLaFichaYElNombreSeCompletaDespues() {
        proyector.alRegistrarsePublicacion(eventoPublicacion("p1"));
        assertThat(almacen.get(CORREO).getNombreCompleto()).isNull();

        proyector.alRegistrarseInvestigador(eventoRegistro());

        assertThat(almacen.get(CORREO).getNombreCompleto()).isEqualTo("Ana Torres");
        assertThat(almacen.get(CORREO).getTotalPublicaciones()).isEqualTo(1);
    }

    @Test
    void unEventoDuplicadoNoDuplicaLaPublicacion() {
        proyector.alRegistrarseInvestigador(eventoRegistro());
        proyector.alRegistrarsePublicacion(eventoPublicacion("p1"));
        proyector.alRegistrarsePublicacion(eventoPublicacion("p1"));

        assertThat(almacen.get(CORREO).getTotalPublicaciones()).isEqualTo(1);
    }

    @Test
    void laEliminacionMarcaLaFichaYLuegoLaBorra() {
        proyector.alRegistrarseInvestigador(eventoRegistro());

        proyector.alIniciarseEliminacion("{\"correoInstitucional\":\"" + CORREO + "\"}");
        assertThat(almacen.get(CORREO).getEstado()).isEqualTo(FichaInvestigador.EN_ELIMINACION);

        proyector.alEliminarseInvestigador("{\"correoInstitucional\":\"" + CORREO + "\"}");
        assertThat(almacen).doesNotContainKey(CORREO);
    }
}
