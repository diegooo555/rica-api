package com.rica.ricaapi.investigadores;

public class InvestigadorMapper {

    private InvestigadorMapper() {
    }

    public static Investigador aEntidad(InvestigadorRequest request) {
        Investigador investigador = new Investigador();
        CorreoInstitucional correoInstitucional = new CorreoInstitucional(request.getCorreoInstitucional());
        investigador.setNombreCompleto(request.getNombreCompleto());
        investigador.setCorreoInstitucional(correoInstitucional);
        investigador.setGrupoInvestigacion(request.getGrupoInvestigacion());
        return investigador;
    }

    public static InvestigadorResponse aResponse(Investigador investigador) {
        return new InvestigadorResponse(
                investigador.getId(),
                investigador.getNombreCompleto(),
                investigador.getGrupoInvestigacion());
    }

}
