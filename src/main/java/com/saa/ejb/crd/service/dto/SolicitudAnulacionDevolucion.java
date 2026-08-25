package com.saa.ejb.crd.service.dto;

/**
 * Body de POST /rest/dvap/anular/{idDevolucion}.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class SolicitudAnulacionDevolucion {

    /** Motivo de la anulación. Obligatorio: queda en CRD.DVAP.DVAPMTAN. */
    private String motivo;

    /** Nombre del usuario que anula. Obligatorio. */
    private String usuario;

    /** Id del usuario que anula, para la traza de la anulación de la orden de pago. */
    private Long idUsuario;

    public SolicitudAnulacionDevolucion() {
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }
}
