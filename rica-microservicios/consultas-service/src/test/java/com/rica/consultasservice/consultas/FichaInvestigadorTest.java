package com.rica.consultasservice.consultas;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FichaInvestigadorTest {

    private static ResumenPublicacion publicacion(String id) {
        return new ResumenPublicacion(id, "Titulo " + id, "articulo", 2026);
    }

    @Test
    void agregarPublicacionActualizaElTotal() {
        FichaInvestigador ficha = FichaInvestigador.nueva("ana.torres@uptc.edu.co");
        ficha.agregarPublicacion(publicacion("p1"));
        ficha.agregarPublicacion(publicacion("p2"));

        assertThat(ficha.getTotalPublicaciones()).isEqualTo(3);
    }

    @Test
    void agregarLaMismaPublicacionDosVecesEsIdempotente() {
        FichaInvestigador ficha = FichaInvestigador.nueva("ana.torres@uptc.edu.co");
        boolean primera = ficha.agregarPublicacion(publicacion("p1"));
        boolean repetida = ficha.agregarPublicacion(publicacion("p1"));

        assertThat(primera).isTrue();
        assertThat(repetida).isFalse();
        assertThat(ficha.getTotalPublicaciones()).isEqualTo(1);
    }

    @Test
    void unaFichaNuevaEstaActivaYSinNombre() {
        FichaInvestigador ficha = FichaInvestigador.nueva("ana.torres@uptc.edu.co");

        assertThat(ficha.getEstado()).isEqualTo(FichaInvestigador.ACTIVO);
        assertThat(ficha.getNombreCompleto()).isNull();
    }
}
