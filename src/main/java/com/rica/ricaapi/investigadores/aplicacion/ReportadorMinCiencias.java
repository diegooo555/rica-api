package com.rica.ricaapi.investigadores.aplicacion;

import com.rica.ricaapi.investigadores.dominio.Investigador;

/**
 * Puerto secundario: notificar a MinCiencias que un investigador se registró.
 * Lo define el núcleo según lo que necesita, no según la tecnología del otro lado.
 */
public interface ReportadorMinCiencias {

    void enviar(Investigador investigador);
}
