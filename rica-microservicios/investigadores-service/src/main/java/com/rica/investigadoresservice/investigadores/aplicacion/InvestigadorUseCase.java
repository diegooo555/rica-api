package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import java.util.List;
import java.util.Optional;

public interface InvestigadorUseCase {

    List<Investigador> listarTodos();

    Investigador buscarPorId(Long id);

    Optional<Investigador> buscarPorCorreo(String correoInstitucional);

    Investigador registrar(String nombreCompleto, String correoInstitucional, String grupoInvestigacion);

    void iniciarEliminacion(Long id);

    void confirmarEliminacion(String correoInstitucional);
}
