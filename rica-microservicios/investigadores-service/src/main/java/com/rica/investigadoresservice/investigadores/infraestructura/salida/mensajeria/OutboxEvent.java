package com.rica.investigadoresservice.investigadores.infraestructura.salida.mensajeria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String topico;

    @Column(length = 150)
    private String clave;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "publicado_en")
    private LocalDateTime publicadoEn;

    protected OutboxEvent() {
    }

    public OutboxEvent(String topico, String clave, String payload) {
        this.topico = topico;
        this.clave = clave;
        this.payload = payload;
        this.creadoEn = LocalDateTime.now();
    }

    public void marcarPublicado() {
        this.publicadoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTopico() { return topico; }
    public String getClave() { return clave; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getPublicadoEn() { return publicadoEn; }
}
