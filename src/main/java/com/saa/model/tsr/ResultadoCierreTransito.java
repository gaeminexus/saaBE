package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author GaemiSoft
 * <p>Respuesta de POST /cnct/transito/cerrar.</p>
 */
public class ResultadoCierreTransito implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idCierre;
    private Long idCuentaBancaria;
    private Long idPeriodo;
    private Double saldoLibros;
    private Double saldoExtracto;
    private Double diferencia;
    private Long estado;
    private LocalDateTime fechaCierre;
    private String usuarioCierre;
    private Integer partidasDeclaradas;

    public Long getIdCierre() { return idCierre; }
    public void setIdCierre(Long idCierre) { this.idCierre = idCierre; }

    public Long getIdCuentaBancaria() { return idCuentaBancaria; }
    public void setIdCuentaBancaria(Long idCuentaBancaria) { this.idCuentaBancaria = idCuentaBancaria; }

    public Long getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(Long idPeriodo) { this.idPeriodo = idPeriodo; }

    public Double getSaldoLibros() { return saldoLibros; }
    public void setSaldoLibros(Double saldoLibros) { this.saldoLibros = saldoLibros; }

    public Double getSaldoExtracto() { return saldoExtracto; }
    public void setSaldoExtracto(Double saldoExtracto) { this.saldoExtracto = saldoExtracto; }

    public Double getDiferencia() { return diferencia; }
    public void setDiferencia(Double diferencia) { this.diferencia = diferencia; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public String getUsuarioCierre() { return usuarioCierre; }
    public void setUsuarioCierre(String usuarioCierre) { this.usuarioCierre = usuarioCierre; }

    public Integer getPartidasDeclaradas() { return partidasDeclaradas; }
    public void setPartidasDeclaradas(Integer partidasDeclaradas) { this.partidasDeclaradas = partidasDeclaradas; }
}
