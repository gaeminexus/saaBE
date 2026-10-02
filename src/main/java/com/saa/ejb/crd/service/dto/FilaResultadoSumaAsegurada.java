package com.saa.ejb.crd.service.dto;

/**
 * Una fila del resultado de {@code POST /posg/sumaAsegurada/carga} (contrato §2, actualizado
 * 2026-10-02 — multipart, el backend lee el Excel).
 */
public class FilaResultadoSumaAsegurada {

    /** Número de fila del Excel, 1-based, contando el encabezado (primera fila de datos = 2). */
    private int fila;
    private Long idAsoprep;
    private Long idPrestamo;
    private Double valorAnterior;
    private Double valor;
    /** OK · NO_EXISTE · NO_ES_HIPOTECARIO_NI_PRENDARIO · VALOR_INVALIDO · DUPLICADO_EN_ARCHIVO. */
    private String resultado;

    public int getFila() { return fila; }
    public void setFila(int fila) { this.fila = fila; }

    public Long getIdAsoprep() { return idAsoprep; }
    public void setIdAsoprep(Long idAsoprep) { this.idAsoprep = idAsoprep; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public Double getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(Double valorAnterior) { this.valorAnterior = valorAnterior; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
}
