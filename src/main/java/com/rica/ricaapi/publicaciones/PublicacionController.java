package com.rica.ricaapi.publicaciones;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @GetMapping
    public List<PublicacionResponse> listarPorInvestigador(@RequestParam String investigadorCorreo) {
        return publicacionService.listarPorInvestigador(investigadorCorreo).stream()
                .map(PublicacionMapper::aResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public PublicacionResponse buscarPorId(@PathVariable String id) {
        Publicacion publicacion = publicacionService.buscarPorId(id);
        return PublicacionMapper.aResponse(publicacion);
    }

    @PostMapping
    public ResponseEntity<PublicacionResponse> registrar(@Valid @RequestBody PublicacionRequest request) {
        Publicacion publicacion = PublicacionMapper.aEntidad(request);
        Publicacion guardada = publicacionService.registrar(publicacion);
        PublicacionResponse response = PublicacionMapper.aResponse(guardada);
        return ResponseEntity
                .created(URI.create("/api/publicaciones/" + guardada.getId()))
                .body(response);
    }

}
