package com.rica.ricaapi.investigadores;

import java.util.List;
import java.util.Optional;

public interface RepositorioInvestigadores {

    List<Investigador> listarTodos();

    Optional<Investigador> buscarPorId(Long id);

    boolean existeCorreo(String correoInstitucional);

    Investigador guardar(Investigador investigador);
}
