package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author GaemiSoft
 * <p>Proyección de una fila pendiente de CNT.DTAS para la preparación del cierre de partidas en
 * tránsito - NO es la entidad completa. Ver ConciliacionCierreService.prepararCierre.</p>
 *
 * <p><b>Corrección del 2026-08-27 (§7bis del diseño):</b> hasta esa fecha, {@code tipoSugerido}
 * venía {@code null} cuando la línea no tenía ningún TSR.MVCB asociado (92% de los casos), porque
 * el ancla de tipo 1/2 era {@code MVCBCDGO}. Ahora {@code idDetalleAsiento} es el ancla —
 * {@code TSR.DTCN.DTCNDTAS} — así que <b>toda</b> línea pendiente de esta proyección es
 * declarable, y {@code tipoSugerido} se deduce siempre del signo del detalle (debe → tipo 1,
 * haber → tipo 2), nunca es null.</p>
 *
 * <p>{@code idMovimientoBanco} sigue pudiendo venir {@code null} — sigue siendo información
 * adicional, no una condición para declarar la partida.</p>
 */
public class PendienteAsientoTransito implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idDetalleAsiento;
    private Long idAsiento;
    private Long idMovimientoBanco;
    private LocalDate fecha;
    private String descripcion;
    private Double valor;
    private boolean esArrastrada;
    private Integer tipoSugerido;

    public Long getIdDetalleAsiento() { return idDetalleAsiento; }
    public void setIdDetalleAsiento(Long idDetalleAsiento) { this.idDetalleAsiento = idDetalleAsiento; }

    public Long getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }

    public Long getIdMovimientoBanco() { return idMovimientoBanco; }
    public void setIdMovimientoBanco(Long idMovimientoBanco) { this.idMovimientoBanco = idMovimientoBanco; }

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
}
