package com.rica.consultasservice.consultas;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada del lado de lectura: escucha los eventos y delega en el proyector. */
@Component
public class ProyeccionListener {

    private final ProyectorFichas proyector;

    public ProyeccionListener(ProyectorFichas proyector) {
        this.proyector = proyector;
    }

    @KafkaListener(topics = "investigadores.registrados")
    public void investigadorRegistrado(String mensaje) {
        proyector.alRegistrarseInvestigador(mensaje);
    }

    @KafkaListener(topics = "publicaciones.registradas")
    public void publicacionRegistrada(String mensaje) {
        proyector.alRegistrarsePublicacion(mensaje);
    }

    @KafkaListener(topics = "investigadores.en-eliminacion")
    public void investigadorEnEliminacion(String mensaje) {
        proyector.alIniciarseEliminacion(mensaje);
    }

    @KafkaListener(topics = "investigadores.eliminados")
    public void investigadorEliminado(String mensaje) {
        proyector.alEliminarseInvestigador(mensaje);
    }
}
