package com.rica.ricaapi.investigadores.aplicacion;

import com.rica.ricaapi.investigadores.dominio.CorreoDuplicadoException;
import com.rica.ricaapi.investigadores.dominio.CorreoInstitucional;
import com.rica.ricaapi.investigadores.dominio.Investigador;

import org.springframework.stereotype.Component;

@Component
public class InvestigadorFactory {

    private final RepositorioInvestigadores repositorioInvestigadores;

    public InvestigadorFactory(RepositorioInvestigadores repositorioInvestigadores) {
        this.repositorioInvestigadores = repositorioInvestigadores;
    }

    public Investigador crear(String nombreCompleto, String correoInstitucional, String grupoInvestigacion) {
        CorreoInstitucional correo = new CorreoInstitucional(correoInstitucional);

        if (repositorioInvestigadores.existeCorreo(correo.valor())) {
            throw new CorreoDuplicadoException(
                    "Ya existe un investigador registrado con el correo " + correo.valor());
        }

        return new Investigador(null, nombreCompleto, correo, grupoInvestigacion);
    }
}