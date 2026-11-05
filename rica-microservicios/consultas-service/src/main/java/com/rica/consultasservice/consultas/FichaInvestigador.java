package com.rica.consultasservice.consultas;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de LECTURA: una ficha por investigador con todo lo que la pantalla necesita
 * (datos del investigador + sus publicaciones) en un solo documento. No es la fuente
 * de verdad: se puede borrar y reconstruir reproduciendo los eventos.
 */
@Document(collection = "fichas_investigador")
public class FichaInvestigador {

    public static final String ACTIVO = "ACTIVO";
    public static final String EN_ELIMINACION = "EN_ELIMINACION";

    @Id
    private String correo;
    private String nombreCompleto; // null hasta que llega el evento de registro
    private String estado;
    private List<ResumenPublicacion> publicaciones;
    private int totalPublicaciones; // guardado (no calculado) para poder ordenar por él en MongoDB

    public FichaInvestigador(String correo, String nombreCompleto, String estado,
                             List<ResumenPublicacion> publicaciones, int totalPublicaciones) {
        this.correo = correo;
        this.nombreCompleto = nombreCompleto;
        this.estado = estado;
        this.publicaciones = publicaciones;
        this.totalPublicaciones = totalPublicaciones;
    }

    public static FichaInvestigador nueva(String correo) {
        return new FichaInvestigador(correo, null, ACTIVO, new ArrayList<>(), 0);
    }

    public void registrar(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public void marcarEnEliminacion() {
        this.estado = EN_ELIMINACION;
    }

    /** Idempotente: si la publicación ya está en la ficha (evento duplicado), no hace nada. */
    public boolean agregarPublicacion(ResumenPublicacion nueva) {
        boolean yaExiste = publicaciones.stream().anyMatch(p -> p.id().equals(nueva.id()));
        if (yaExiste) {
            return false;
        }
        publicaciones.add(nueva);
        totalPublicaciones = publicaciones.size();
        return true;
    }

    public String getCorreo() { return correo; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getEstado() { return estado; }
    public List<ResumenPublicacion> getPublicaciones() { return publicaciones; }
    public int getTotalPublicaciones() { return totalPublicaciones; }
}
