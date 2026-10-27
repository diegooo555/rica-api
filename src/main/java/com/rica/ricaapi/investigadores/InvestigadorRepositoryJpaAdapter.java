package com.rica.ricaapi.investigadores;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class InvestigadorRepositoryJpaAdapter implements RepositorioInvestigadores {

    private final InvestigadorRepository investigadorRepository;

    public InvestigadorRepositoryJpaAdapter(InvestigadorRepository investigadorRepository) {
        this.investigadorRepository = investigadorRepository;
    }

    @Override
    public List<Investigador> listarTodos() {
        return investigadorRepository.findAll();
    }

    @Override
    public Optional<Investigador> buscarPorId(Long id) {
        return investigadorRepository.findById(id);
    }

    @Override
    public boolean existeCorreo(String correoInstitucional) {
        return investigadorRepository.existsByCorreoInstitucional_Valor(correoInstitucional);
    }

    @Override
    public Investigador guardar(Investigador investigador) {
        return investigadorRepository.save(investigador);
    }
}
