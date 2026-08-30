package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author GaemiSoft
 * <p>Fila de GET /cnct/transito/antiguas/{idEmpresa} - riesgo #1 del diseño: una partida en
 * tránsito que nunca se salda es un síntoma, no un dato.</p>
 */
public class PartidaTransitoAntigua implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idPartida;
    private Integer tipo;
    private Double valor;
    private Long diasEnTransito;
    private String cuentaBancaria;
    private LocalDateTime declaradaEn;
    private String observacion;

    public Long getIdPartida() { return idPartida; }
    public void setIdPartida(Long idPartida) { this.idPartida = idPartida; }

    public Integer getTipo() { return tipo; }
    public void setTipo(Integer tipo) { this.tipo = tipo; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public Long getDiasEnTransito() { return diasEnTransito; }
    public void setDiasEnTransito(Long diasEnTransito) { this.diasEnTransito = diasEnTransito; }

    public String getCuentaBancaria() { return cuentaBancaria; }
    public void setCuentaBancaria(String cuentaBancaria) { this.cuentaBancaria = cuentaBancaria; }

    public LocalDateTime getDeclaradaEn() { return declaradaEn; }
    public void setDeclaradaEn(LocalDateTime declaradaEn) { this.declaradaEn = declaradaEn; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
