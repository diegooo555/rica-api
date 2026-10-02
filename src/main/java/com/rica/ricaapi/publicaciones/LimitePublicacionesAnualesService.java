package com.rica.ricaapi.publicaciones;

import com.rica.ricaapi.investigadores.Investigador;
import org.springframework.stereotype.Service;

@Service
public class LimitePublicacionesAnualesService {

    private static final int MAXIMO_POR_ANIO = 5;

    private final PublicacionRepository publicacionRepository;

    public LimitePublicacionesAnualesService(PublicacionRepository publicacionRepository) {
        this.publicacionRepository = publicacionRepository;
    }

    public boolean puedeRegistrar(Investigador investigador, Publicacion nueva) {
        long registradasEsteAnio = publicacionRepository.countByInvestigadorCorreoAndAnio(
                investigador.getCorreoInstitucional().valor(), nueva.getAnio());
        return registradasEsteAnio < MAXIMO_POR_ANIO;
    }
}