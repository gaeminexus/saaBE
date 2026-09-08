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
 *
 * <p><b>Agregado el 2026-09-07</b>, para poder rastrear una línea pendiente hasta su origen real
 * (pedido del usuario en la pantalla de Conciliación — Cierre): {@code numeroAlternoAsiento} /
 * {@code numeroAsiento} / {@code observacionAsiento} salen directo de {@code CNT.ASNT}, ya cargado
 * junto con el detalle — no piden consulta adicional. {@code origen} / {@code idOrigen} /
 * {@code referenciaBanco} / {@code idPago} salen de {@code PGS.PGTR.PGTRASNT}, resuelto en una
 * sola consulta para todos los asientos del lote (ver {@code prepararCierre}) — nunca uno por
 * fila. <b>Sólo vienen poblados si el asiento lo generó un pago de origen externo</b> (anticipo a
 * empleado, caja chica, u otro origen externo con desglose): un asiento de factura de compra o de
 * egreso directo cuelga su asiento de otro documento (la aplicación de pago, el propio egreso), no
 * de {@code PGTRASNT}, así que estos cuatro campos vienen {@code null} para esos casos — no es un
 * error, es que ese camino no pasa por aquí.</p>
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
    private String numeroAlternoAsiento;
    private Long numeroAsiento;
    private String observacionAsiento;
    private String origen;
    private Long idOrigen;
    private String referenciaBanco;
    private Long idPago;

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

    public String getNumeroAlternoAsiento() { return numeroAlternoAsiento; }
    public void setNumeroAlternoAsiento(String numeroAlternoAsiento) { this.numeroAlternoAsiento = numeroAlternoAsiento; }

    public Long getNumeroAsiento() { return numeroAsiento; }
    public void setNumeroAsiento(Long numeroAsiento) { this.numeroAsiento = numeroAsiento; }

    public String getObservacionAsiento() { return observacionAsiento; }
    public void setObservacionAsiento(String observacionAsiento) { this.observacionAsiento = observacionAsiento; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public Long getIdOrigen() { return idOrigen; }
    public void setIdOrigen(Long idOrigen) { this.idOrigen = idOrigen; }

    public String getReferenciaBanco() { return referenciaBanco; }
    public void setReferenciaBanco(String referenciaBanco) { this.referenciaBanco = referenciaBanco; }

    public Long getIdPago() { return idPago; }
    public void setIdPago(Long idPago) { this.idPago = idPago; }
}
