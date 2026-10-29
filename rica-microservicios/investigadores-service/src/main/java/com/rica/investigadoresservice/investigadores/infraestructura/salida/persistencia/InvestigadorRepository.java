package com.rica.investigadoresservice.investigadores.infraestructura.salida.persistencia;

import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestigadorRepository extends JpaRepository<Investigador, Long> {

    boolean existsByCorreoInstitucional_Valor(String valor);

}
