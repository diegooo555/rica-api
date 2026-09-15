package com.rica.ricaapi.publicaciones;

public class PublicacionMapper {

    public static Publicacion aEntidad(PublicacionRequest request) {
        Publicacion publicacion = new Publicacion();
        publicacion.setInvestigadorCorreo(request.getInvestigadorCorreo());
        publicacion.setTitulo(request.getTitulo());
        publicacion.setTipo(request.getTipo());
        publicacion.setAnio(request.getAnio());
        publicacion.setDetalles(request.getDetalles());
        return publicacion;
    }

    public static PublicacionResponse aResponse(Publicacion publicacion) {
        PublicacionResponse response = new PublicacionResponse();
        response.setInvestigadorCorreo(publicacion.getInvestigadorCorreo());
        response.setTitulo(publicacion.getTitulo());
        response.setTipo(publicacion.getTipo());
        response.setAnio(publicacion.getAnio());
        response.setDetalles(publicacion.getDetalles());
        return response;
    }

}
