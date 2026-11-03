package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rica.investigadoresservice.compartido.RecursoNoEncontradoException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InvestigadorService implements InvestigadorUseCase {

    private final RepositorioInvestigadores repositorioInvestigadores;
    private final InvestigadorFactory investigadorFactory;
    private final ReportadorMinCiencias reportadorMinCiencias;
    private final PublicadorEventos publicadorEventos;

    public InvestigadorService(RepositorioInvestigadores repositorioInvestigadores, InvestigadorFactory investigadorFactory,
            ReportadorMinCiencias reportadorMinCiencias, PublicadorEventos publicadorEventos) {
        this.repositorioInvestigadores = repositorioInvestigadores;
        this.investigadorFactory = investigadorFactory;
        this.reportadorMinCiencias = reportadorMinCiencias;
        this.publicadorEventos = publicadorEventos;
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
    public Optional<Investigador> buscarPorCorreo(String correoInstitucional) {
        return repositorioInvestigadores.buscarPorCorreo(correoInstitucional);
    }

    @Override
    @Transactional
    public Investigador registrar(String nombreCompleto, String correoInstitucional, String grupoInvestigacion) {
        Investigador investigador = investigadorFactory.crear(nombreCompleto, correoInstitucional, grupoInvestigacion);
        Investigador guardado = repositorioInvestigadores.guardar(investigador);
        // El evento se guarda en el outbox dentro de esta misma transacción.
        publicadorEventos.publicar("investigadores.registrados", guardado.getCorreoInstitucional().valor(), Map.of(
                "id", guardado.getId(),
                "correoInstitucional", guardado.getCorreoInstitucional().valor(),
                "nombreCompleto", guardado.getNombreCompleto(),
                "grupoInvestigacion", guardado.getGrupoInvestigacion()));
        reportadorMinCiencias.enviar(guardado);
        return guardado;
    }

}