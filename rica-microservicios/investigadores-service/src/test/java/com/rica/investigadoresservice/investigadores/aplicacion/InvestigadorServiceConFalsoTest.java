package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.investigadores.dominio.CorreoDuplicadoException;
import com.rica.investigadoresservice.investigadores.dominio.Investigador;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvestigadorServiceConFalsoTest {

    @Test
    void registraYRecuperaUnInvestigadorSinNingunaDependenciaDeSpringNiDeBaseDeDatos() {
        RepositorioInvestigadoresFalso repositorio = new RepositorioInvestigadoresFalso();
        InvestigadorFactory factory = new InvestigadorFactory(repositorio);
        InvestigadorService service = new InvestigadorService(repositorio, factory, investigador -> { }, (topico, clave, contenido) -> { });

        Investigador guardado = service.registrar("Ana Torres", "ana.torres@uptc.edu.co", "GIT-UPTC");

        assertThat(guardado.getId()).isNotNull();
        assertThat(service.buscarPorId(guardado.getId()).getNombreCompleto()).isEqualTo("Ana Torres");
    }

    @Test
    void rechazaUnSegundoRegistroConElMismoCorreo() {
        RepositorioInvestigadoresFalso repositorio = new RepositorioInvestigadoresFalso();
        InvestigadorService service = new InvestigadorService(repositorio, new InvestigadorFactory(repositorio), investigador -> { }, (topico, clave, contenido) -> { });
        service.registrar("Ana Torres", "ana.torres@uptc.edu.co", "GIT-UPTC");

        assertThatThrownBy(() -> service.registrar("Ana T.", "ana.torres@uptc.edu.co", "GIT-UPTC"))
                .isInstanceOf(CorreoDuplicadoException.class);
        assertThat(service.listarTodos()).hasSize(1);
    }
}
