package com.rica.consultasservice.consultas;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/** Cuenta cuántas altas de investigadores se han visto. OJO: incrementar NO es idempotente. */
@Service
public class ContadorAltas {

    private static final String ID = "global";

    private final MongoTemplate mongoTemplate;

    public ContadorAltas(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void contarAlta() {
        mongoTemplate.upsert(
                Query.query(Criteria.where("_id").is(ID)),
                new Update().inc("investigadoresRegistrados", 1),
                Estadisticas.class);
    }

    public long total() {
        Estadisticas estadisticas = mongoTemplate.findById(ID, Estadisticas.class);
        return estadisticas == null ? 0 : estadisticas.getInvestigadoresRegistrados();
    }
}
