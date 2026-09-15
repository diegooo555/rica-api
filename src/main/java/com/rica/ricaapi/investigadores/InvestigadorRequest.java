package com.rica.ricaapi.investigadores;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class InvestigadorRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "El correo institucional debe tener un formato válido")
    private String correoInstitucional;

    @NotBlank(message = "El grupo de investigación es obligatorio")
    private String grupoInvestigacion;

    public InvestigadorRequest() {
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreoInstitucional() {
        return correoInstitucional;
    }

    public void setCorreoInstitucional(String correoInstitucional) {
        this.correoInstitucional = correoInstitucional;
    }

    public String getGrupoInvestigacion() {
        return grupoInvestigacion;
    }

    public void setGrupoInvestigacion(String grupoInvestigacion) {
        this.grupoInvestigacion = grupoInvestigacion;
    }

}