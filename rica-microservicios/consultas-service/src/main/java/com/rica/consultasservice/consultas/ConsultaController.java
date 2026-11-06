package com.rica.consultasservice.consultas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/consultas")
public class ConsultaController {

    private final ServicioConsultas servicioConsultas;

    public ConsultaController(ServicioConsultas servicioConsultas) {
        this.servicioConsultas = servicioConsultas;
    }

    @GetMapping("/investigadores")
    public List<FichaResponse> listar(@RequestParam(required = false) String estado) {
        return servicioConsultas.listar(estado);
    }

    @GetMapping("/investigadores/{correo}")
    public FichaResponse buscarPorCorreo(@PathVariable String correo) {
        return servicioConsultas.buscarPorCorreo(correo);
    }

    @GetMapping("/ranking")
    public List<FichaResponse> ranking(@RequestParam(defaultValue = "3") int top) {
        return servicioConsultas.ranking(top);
    }
}
