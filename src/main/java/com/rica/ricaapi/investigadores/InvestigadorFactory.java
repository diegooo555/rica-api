package com.rica.ricaapi.investigadores;

import org.springframework.stereotype.Component;

@Component
public class InvestigadorFactory {

    private final InvestigadorRepository investigadorRepository;

    public InvestigadorFactory(InvestigadorRepository investigadorRepository) {
        this.investigadorRepository = investigadorRepository;
    }

    public Investigador crear(String nombreCompleto, String correoInstitucional, String grupoInvestigacion) {
        CorreoInstitucional correo = new CorreoInstitucional(correoInstitucional);

        if (investigadorRepository.existsByCorreoInstitucional_Valor(correo.valor())) {
            throw new CorreoDuplicadoException(
                    "Ya existe un investigador registrado con el correo " + correo.valor());
        }

        return new Investigador(null, nombreCompleto, correo, grupoInvestigacion);
    }
}