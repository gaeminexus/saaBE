package com.saa.ejb.crd.service.dto;

/**
 * Body de {@code POST /rest/crar/reprocesar/{idCarga}} — reproceso de una carga Petro ya
 * aplicada (cuadre Superintendencia, cambio C4). Ver
 * {@code docs/logica-negocio/petro/REPROCESO-CARGA-PETRO.md}.
 *
 * <p>{@code usuario} y {@code motivo} son OBLIGATORIOS: el motivo queda estampado en cada
 * pago anulado ({@code PGPRMTAN}), en cada contra-movimiento de aporte ({@code APRTGLSA}) y
 * en cada asiento anulado ({@code ASNTMTAN}), junto con la etiqueta de corrida.</p>
 */
public class SolicitudReprocesoCargaPetro {

    private String usuario;
    private String ip;
    private String motivo;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
