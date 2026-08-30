package com.saa.model.tsr;

import java.io.Serializable;

/**
 * @author GaemiSoft
 * <p>DTO del cuerpo JSON para POST /cnct/transito/anular/{idCierre}.</p>
 */
public class SolicitudAnularCierre implements Serializable {

    private static final long serialVersionUID = 1L;

    private String motivo;
    private Long idUsuario;

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
}
