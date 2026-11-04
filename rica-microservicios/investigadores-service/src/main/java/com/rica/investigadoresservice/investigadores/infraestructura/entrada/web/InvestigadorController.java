package com.rica.investigadoresservice.investigadores.infraestructura.entrada.web;

import com.rica.investigadoresservice.investigadores.aplicacion.InvestigadorUseCase;
import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/investigadores")
public class InvestigadorController {

    private final InvestigadorUseCase investigadorService;

    public InvestigadorController(InvestigadorUseCase investigadorService) {
        this.investigadorService = investigadorService;
    }

    @GetMapping
    public List<InvestigadorResponse> listar() {
        return investigadorService.listarTodos().stream()
                .map(InvestigadorMapper::aResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public InvestigadorResponse buscarPorId(@PathVariable Long id) {
        Investigador investigador = investigadorService.buscarPorId(id);
        return InvestigadorMapper.aResponse(investigador);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        // 202: la eliminación es una Saga asíncrona, termina cuando publicaciones-service confirme.
        investigadorService.iniciarEliminacion(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping
    public ResponseEntity<InvestigadorResponse> registrar(@Valid @RequestBody InvestigadorRequest request) {
        Investigador investigador = InvestigadorMapper.aEntidad(request);
        Investigador guardado = investigadorService.registrar(investigador.getNombreCompleto(),
                investigador.getCorreoInstitucional().valor(), investigador.getGrupoInvestigacion());
        InvestigadorResponse response = InvestigadorMapper.aResponse(guardado);
        return ResponseEntity
                .created(URI.create("/api/investigadores/" + guardado.getId()))
                .body(response);
    }

}