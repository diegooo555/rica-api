package com.rica.ricaapi.investigadores;

import org.springframework.stereotype.Service;

import com.rica.ricaapi.compartido.RecursoNoEncontradoException;

import java.util.List;

@Service
public class InvestigadorService {

    private final InvestigadorRepository investigadorRepository;

    public InvestigadorService(InvestigadorRepository investigadorRepository) {
        this.investigadorRepository = investigadorRepository;
    }

    public List<Investigador> listarTodos() {
        return investigadorRepository.findAll();
    }

    public Investigador buscarPorId(Long id) {
        return investigadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un investigador con id " + id));
    }

    public Investigador registrar(Investigador investigador) {
        if (investigadorRepository.existsByCorreoInstitucional(investigador.getCorreoInstitucional())) {
            throw new CorreoDuplicadoException(
                    "Ya existe un investigador registrado con el correo " + investigador.getCorreoInstitucional());
        }
        return investigadorRepository.save(investigador);
    }

}