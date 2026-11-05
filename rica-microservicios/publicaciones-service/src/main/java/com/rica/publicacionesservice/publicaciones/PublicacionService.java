package com.rica.publicacionesservice.publicaciones;

import org.springframework.stereotype.Service;

import com.rica.publicacionesservice.compartido.RecursoNoEncontradoException;

import java.util.List;

@Service
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final VerificadorInvestigador verificadorInvestigador;
    private final LimitePublicacionesAnualesService limitePublicacionesAnualesService;
    private final PublicadorEventosPublicaciones publicadorEventos;

    public PublicacionService(PublicacionRepository publicacionRepository,
            VerificadorInvestigador verificadorInvestigador,
            LimitePublicacionesAnualesService limitePublicacionesAnualesService,
            PublicadorEventosPublicaciones publicadorEventos) {
        this.publicacionRepository = publicacionRepository;
        this.verificadorInvestigador = verificadorInvestigador;
        this.limitePublicacionesAnualesService = limitePublicacionesAnualesService;
        this.publicadorEventos = publicadorEventos;
    }

    public Publicacion registrar(Publicacion publicacion) {
        if (!verificadorInvestigador.existe(publicacion.getInvestigadorCorreo())) {
            throw new RecursoNoEncontradoException(
                    "No existe un investigador con correo " + publicacion.getInvestigadorCorreo());
        }
        if (!limitePublicacionesAnualesService.puedeRegistrar(publicacion.getInvestigadorCorreo(), publicacion.getAnio())) {
            throw new LimiteAnualExcedidoException(
                    "El investigador " + publicacion.getInvestigadorCorreo()
                            + " ya alcanzó el máximo de publicaciones registradas para " + publicacion.getAnio());
        }
        Publicacion guardada = publicacionRepository.save(publicacion);
        // Solo se anuncia lo que de verdad ocurrió: si el comando falló arriba, no hay evento.
        publicadorEventos.publicacionRegistrada(guardada);
        return guardada;
    }

    public List<Publicacion> listarPorInvestigador(String investigadorCorreo) {
        return publicacionRepository.findByInvestigadorCorreo(investigadorCorreo);
    }

    public Publicacion buscarPorId(String id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una publicación con id " + id));
    }

}
