package com.rica.ricaapi.investigadores;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestigadorRepository extends JpaRepository<Investigador, Long> {

    boolean existsByCorreoInstitucional(String correoInstitucional);

    Optional<Investigador> existsByCorreoInstitucional_Valor(String valor);

}