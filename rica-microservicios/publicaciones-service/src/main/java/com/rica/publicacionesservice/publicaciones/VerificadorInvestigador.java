package com.rica.publicacionesservice.publicaciones;

/**
 * Puerto secundario: lo que publicaciones necesita saber de investigadores,
 * diseñado por quien lo consume y no por la tecnología del otro lado.
 */
public interface VerificadorInvestigador {

    boolean existe(String correoInstitucional);
}
