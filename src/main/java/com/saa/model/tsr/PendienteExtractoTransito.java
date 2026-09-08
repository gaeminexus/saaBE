package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author GaemiSoft
 * <p>Proyección de una fila pendiente de TSR.DEXB para la preparación del cierre de partidas en
 * tránsito - NO es la entidad completa (evita arrastrar cuentaBancaria→banco→empresa). Ver
 * ConciliacionCierreService.prepararCierre.</p>
 *
 * <p><b>Agregado el 2026-09-07:</b> {@code referencia} sale de {@code TSR.DEXB.DEXBREFR}
 * ("referencia o número de documento del banco", según el javadoc de la entidad). Es lo único que
 * identifica el movimiento del lado del extracto además de la descripción — verificado contra
 * {@code DetalleExtractoBancario}: no hay ninguna otra columna de identificación (número de cheque,
 * de documento) del lado del extracto. {@code codigoMovimiento} (DEXBCDMV) también existe, pero es
 * un código de categoría del banco (TW, DP, N/C, CABE...), no un identificador de este movimiento
 * puntual — no se agrega acá.</p>
 */
public class PendienteExtractoTransito implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idDetalleExtracto;
    private LocalDate fecha;
    private String descripcion;
    private Double valor;
    private boolean esArrastrada;
    private Integer tipoSugerido;
    private String referencia;

    public Long getIdDetalleExtracto() { return idDetalleExtracto; }
    public void setIdDetalleExtracto(Long idDetalleExtracto) { this.idDetalleExtracto = idDetalleExtracto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public boolean isEsArrastrada() { return esArrastrada; }
    public void setEsArrastrada(boolean esArrastrada) { this.esArrastrada = esArrastrada; }

    public Integer getTipoSugerido() { return tipoSugerido; }
    public void setTipoSugerido(Integer tipoSugerido) { this.tipoSugerido = tipoSugerido; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
