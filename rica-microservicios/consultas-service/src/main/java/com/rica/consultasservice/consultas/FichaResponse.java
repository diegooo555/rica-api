package com.rica.consultasservice.consultas;

import java.util.List;

public record FichaResponse(String correo, String nombreCompleto, String estado,
                            int totalPublicaciones, List<ResumenPublicacion> publicaciones) {

    public static FichaResponse desde(FichaInvestigador ficha) {
        return new FichaResponse(ficha.getCorreo(), ficha.getNombreCompleto(), ficha.getEstado(),
                ficha.getTotalPublicaciones(), ficha.getPublicaciones());
    }
}
