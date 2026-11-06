package com.rica.consultasservice.consultas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * El "proyector": traduce cada evento del lado de escritura en un cambio sobre el
 * modelo de lectura. Los métodos son synchronized porque cada tópico tiene su propio
 * hilo consumidor y dos eventos del mismo investigador podrían pisarse al leer-modificar-guardar
 * (con varias instancias haría falta bloqueo optimista, @Version).
 */
@Service
public class ProyectorFichas {

    private static final Logger log = LoggerFactory.getLogger(ProyectorFichas.class);

    private final FichaRepository fichaRepository;
    private final ObjectMapper objectMapper;

    public ProyectorFichas(FichaRepository fichaRepository, ObjectMapper objectMapper) {
        this.fichaRepository = fichaRepository;
        this.objectMapper = objectMapper;
    }

    public synchronized void alRegistrarseInvestigador(String mensaje) {
        JsonNode evento = objectMapper.readTree(mensaje);
        String correo = evento.get("correoInstitucional").asString();
        FichaInvestigador ficha = fichaOCrear(correo);
        ficha.registrar(evento.get("nombreCompleto").asString());
        fichaRepository.save(ficha);
        log.info("Proyección: ficha de {} actualizada (registro)", correo);
    }

    public synchronized void alRegistrarsePublicacion(String mensaje) {
        JsonNode evento = objectMapper.readTree(mensaje);
        String correo = evento.get("investigadorCorreo").asString();
        // Si este evento llega ANTES que el de registro (tópicos distintos, sin orden entre ellos),
        // se crea la ficha vacía y el nombre se completa cuando llegue el otro evento.
        FichaInvestigador ficha = fichaOCrear(correo);
        boolean agregada = ficha.agregarPublicacion(new ResumenPublicacion(
                evento.get("id").asString(),
                evento.get("titulo").asString(),
                evento.get("tipo").asString(),
                evento.get("anio").asInt()));
        if (agregada) {
            fichaRepository.save(ficha);
            log.info("Proyección: ficha de {} ahora tiene {} publicaciones", correo, ficha.getTotalPublicaciones());
        } else {
            log.info("Proyección: evento duplicado ignorado para {}", correo);
        }
    }

    public synchronized void alIniciarseEliminacion(String mensaje) {
        String correo = objectMapper.readTree(mensaje).get("correoInstitucional").asString();
        FichaInvestigador ficha = fichaOCrear(correo);
        ficha.marcarEnEliminacion();
        fichaRepository.save(ficha);
        log.info("Proyección: {} marcado EN_ELIMINACION", correo);
    }

    public synchronized void alEliminarseInvestigador(String mensaje) {
        String correo = objectMapper.readTree(mensaje).get("correoInstitucional").asString();
        fichaRepository.deleteById(correo);
        log.info("Proyección: ficha de {} eliminada", correo);
    }

    private FichaInvestigador fichaOCrear(String correo) {
        return fichaRepository.findById(correo).orElseGet(() -> FichaInvestigador.nueva(correo));
    }
}
