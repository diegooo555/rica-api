package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rica.investigadoresservice.compartido.RecursoNoEncontradoException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InvestigadorService implements InvestigadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(InvestigadorService.class);

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

    /** Paso 1 de la Saga (coreografía): bloquear al investigador y avisar a los demás servicios. */
    @Override
    @Transactional
    public void iniciarEliminacion(Long id) {
        Investigador investigador = buscarPorId(id);
        if (investigador.estaEnEliminacion()) {
            return; // la Saga ya está en curso: repetir la petición no publica otro evento
        }
        log.info("Saga: inicia la eliminación de {}", investigador.getCorreoInstitucional().valor());
        investigador.marcarEnEliminacion();
        repositorioInvestigadores.guardar(investigador);
        publicadorEventos.publicar("investigadores.en-eliminacion", investigador.getCorreoInstitucional().valor(), Map.of(
                "id", investigador.getId(),
                "correoInstitucional", investigador.getCorreoInstitucional().valor()));
    }

    /** Paso 3 de la Saga: publicaciones-service confirmó, ahora sí se borra. Idempotente. */
    @Override
    @Transactional
    public void confirmarEliminacion(String correoInstitucional) {
        repositorioInvestigadores.buscarPorCorreo(correoInstitucional)
                .filter(Investigador::estaEnEliminacion)
                .ifPresent(investigador -> {
                    repositorioInvestigadores.eliminar(investigador);
                    // Los consumidores (p. ej. consultas-service) se enteran de que ya no existe.
                    publicadorEventos.publicar(
                            "investigadores.eliminados",
                            correoInstitucional,
                            Map.of("correoInstitucional", correoInstitucional));
                });
    }

}
