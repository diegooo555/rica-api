package com.rica.publicacionesservice.publicaciones;

public class PublicacionMapper {

    private PublicacionMapper() {
    }

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
        response.setId(publicacion.getId());
        response.setInvestigadorCorreo(publicacion.getInvestigadorCorreo());
        response.setTitulo(publicacion.getTitulo());
        response.setTipo(publicacion.getTipo());
        response.setAnio(publicacion.getAnio());
        response.setDetalles(publicacion.getDetalles());
        response.setHuerfana(publicacion.isHuerfana());
        return response;
    }

}
