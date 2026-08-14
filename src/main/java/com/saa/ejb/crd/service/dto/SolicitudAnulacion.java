package com.saa.ejb.crd.service.dto;

/**
 * Body de POST /rest/prst/anularOperacion. Anula un EventoPrestamo completo.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §7.6.
 */
public class SolicitudAnulacion {

    private Long idEvento;
    private String usuario;
    private String motivo;

    public SolicitudAnulacion() {
    }

    public Long getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
