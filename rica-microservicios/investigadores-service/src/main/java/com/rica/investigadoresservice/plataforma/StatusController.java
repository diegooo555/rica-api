package com.rica.investigadoresservice.plataforma;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatusController {

    @GetMapping("/api/status")
    public Map<String, String> status() {
        return Map.of(
                "servicio", "investigadores-service",
                "estado", "operativo");
    }

}
