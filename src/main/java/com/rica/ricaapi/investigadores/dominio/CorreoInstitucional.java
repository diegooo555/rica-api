package com.rica.ricaapi.investigadores.dominio;

import jakarta.persistence.Embeddable;

@Embeddable
public record CorreoInstitucional(String valor) {

  public CorreoInstitucional {
    if (valor == null || !valor.endsWith("@uptc.edu.co")) {
      throw new IllegalArgumentException(
        "El correo institucional debe pertenecer al dominio @uptc.edu.co");
    }
  }
}