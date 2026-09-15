package com.rica.ricaapi.investigadores;

public class InvestigadorResponse {

    private Long id;
    private String nombreCompleto;
    private String grupoInvestigacion;

    public InvestigadorResponse() {
    }

    public InvestigadorResponse(Long id, String nombreCompleto, String grupoInvestigacion) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.grupoInvestigacion = grupoInvestigacion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getGrupoInvestigacion() {
        return grupoInvestigacion;
    }

    public void setGrupoInvestigacion(String grupoInvestigacion) {
        this.grupoInvestigacion = grupoInvestigacion;
    }

}