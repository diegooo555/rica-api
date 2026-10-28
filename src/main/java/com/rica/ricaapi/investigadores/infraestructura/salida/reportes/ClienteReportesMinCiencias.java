package com.rica.ricaapi.investigadores.infraestructura.salida.reportes;

import com.rica.ricaapi.investigadores.aplicacion.ReportadorMinCiencias;
import com.rica.ricaapi.investigadores.dominio.Investigador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador secundario simulado: por ahora solo deja constancia en el log.
 * Una integración real con MinCiencias reemplazaría esta clase sin tocar el núcleo.
 */
@Component
public class ClienteReportesMinCiencias implements ReportadorMinCiencias {

    private static final Logger log = LoggerFactory.getLogger(ClienteReportesMinCiencias.class);

    @Override
    public void enviar(Investigador investigador) {
        log.info("Reporte a MinCiencias (simulado): investigador {} registrado con correo {}",
                investigador.getId(), investigador.getCorreoInstitucional().valor());
    }
}
