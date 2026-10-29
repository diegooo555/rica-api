package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import org.springframework.stereotype.Service;

import com.rica.investigadoresservice.compartido.RecursoNoEncontradoException;

import java.util.List;

@Service
public class InvestigadorService implements InvestigadorUseCase {

    private final RepositorioInvestigadores repositorioInvestigadores;
    private final InvestigadorFactory investigadorFactory;
    private final ReportadorMinCiencias reportadorMinCiencias;

    public InvestigadorService(RepositorioInvestigadores repositorioInvestigadores, InvestigadorFactory investigadorFactory,
            ReportadorMinCiencias reportadorMinCiencias) {
        this.repositorioInvestigadores = repositorioInvestigadores;
        this.investigadorFactory = investigadorFactory;
        this.reportadorMinCiencias = reportadorMinCiencias;
    }

    @Override
    public List<Investigador> listarTodos() {
        return repositorioInvestigadores.listarTodos();
    }

    @Override
    public Investigador buscarPorId(Long id) {
        return repositorioInvestigadores.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un investigador con id " + id));
    }

    @Override
    public Investigador registrar(String nombreCompleto, String correoInstitucional, String grupoInvestigacion) {
        Investigador investigador = investigadorFactory.crear(nombreCompleto, correoInstitucional, grupoInvestigacion);
        Investigador guardado = repositorioInvestigadores.guardar(investigador);
        reportadorMinCiencias.enviar(guardado);
        return guardado;
    }

}