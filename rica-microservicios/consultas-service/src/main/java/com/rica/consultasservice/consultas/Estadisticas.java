package com.rica.consultasservice.consultas;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Un único documento ("global") con contadores del lado de lectura. */
@Document(collection = "estadisticas")
public class Estadisticas {

    @Id
    private String id;
    private long investigadoresRegistrados;

    public Estadisticas(String id, long investigadoresRegistrados) {
        this.id = id;
        this.investigadoresRegistrados = investigadoresRegistrados;
    }

    public String getId() { return id; }
    public long getInvestigadoresRegistrados() { return investigadoresRegistrados; }
}
