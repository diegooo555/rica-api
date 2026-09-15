package com.rica.ricaapi.investigadores;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestigadorRepository extends JpaRepository<Investigador, Long> {

    boolean existsByCorreoInstitucional(String correoInstitucional);

}