package com.saa.model.tsr;

import java.io.Serializable;
import java.util.List;

/**
 * @author GaemiSoft
 * <p>DTO del cuerpo JSON para POST /cnct/transito/cerrar.</p>
 */
public class SolicitudCierreTransito implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idCuentaBancaria;
    private Long idPeriodo;
    private List<PartidaTransitoSolicitud> partidas;
    private Double saldoExtracto;
    private Long idUsuario;

    public Long getIdCuentaBancaria() { return idCuentaBancaria; }
    public void setIdCuentaBancaria(Long idCuentaBancaria) { this.idCuentaBancaria = idCuentaBancaria; }

    public Long getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(Long idPeriodo) { this.idPeriodo = idPeriodo; }

    public List<PartidaTransitoSolicitud> getPartidas() { return partidas; }
    public void setPartidas(List<PartidaTransitoSolicitud> partidas) { this.partidas = partidas; }

    public Double getSaldoExtracto() { return saldoExtracto; }
    public void setSaldoExtracto(Double saldoExtracto) { this.saldoExtracto = saldoExtracto; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
}
