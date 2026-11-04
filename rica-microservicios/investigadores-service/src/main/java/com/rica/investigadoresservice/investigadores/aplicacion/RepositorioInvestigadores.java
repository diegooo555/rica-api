package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import java.util.List;
import java.util.Optional;

public interface RepositorioInvestigadores {

    List<Investigador> listarTodos();

    Optional<Investigador> buscarPorId(Long id);

    Optional<Investigador> buscarPorCorreo(String correoInstitucional);

    boolean existeCorreo(String correoInstitucional);

    Investigador guardar(Investigador investigador);

    void eliminar(Investigador investigador);
}
