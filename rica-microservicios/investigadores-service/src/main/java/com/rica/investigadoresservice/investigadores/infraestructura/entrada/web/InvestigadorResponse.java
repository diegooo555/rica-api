package com.rica.investigadoresservice.investigadores.infraestructura.entrada.web;

public class InvestigadorResponse {

    private Long id;
    private String nombreCompleto;
    private String grupoInvestigacion;
    private String estado;

    public InvestigadorResponse() {
    }

    public InvestigadorResponse(Long id, String nombreCompleto, String grupoInvestigacion, String estado) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.grupoInvestigacion = grupoInvestigacion;
        this.estado = estado;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

}
