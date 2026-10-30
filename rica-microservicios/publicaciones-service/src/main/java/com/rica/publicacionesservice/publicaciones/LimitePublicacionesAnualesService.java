package com.rica.publicacionesservice.publicaciones;

import org.springframework.stereotype.Service;

/**
 * Servicio de dominio: un investigador no puede registrar más de cinco publicaciones por año.
 * Recibe el correo en lugar de la entidad Investigador, que ahora vive en otro servicio.
 */
@Service
public class LimitePublicacionesAnualesService {

    private static final int MAXIMO_POR_ANIO = 5;

    private final PublicacionRepository publicacionRepository;

    public LimitePublicacionesAnualesService(PublicacionRepository publicacionRepository) {
        this.publicacionRepository = publicacionRepository;
    }

    public boolean puedeRegistrar(String investigadorCorreo, Integer anio) {
        long registradasEsteAnio = publicacionRepository.countByInvestigadorCorreoAndAnio(investigadorCorreo, anio);
        return registradasEsteAnio < MAXIMO_POR_ANIO;
    }
}
