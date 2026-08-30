package com.saa.model.tsr;

import java.io.Serializable;

/**
 * @author GaemiSoft
 * <p>Una partida a declarar en tránsito, dentro del cuerpo de POST /cnct/transito/cerrar. Exactamente
 * uno de {@code idDetalleAsiento}/{@code idDetalleExtracto} debe venir informado, según el tipo
 * (mismo XOR que CK_DTCN_ORIGEN): tipo 1/2 → idDetalleAsiento; tipo 3/4 → idDetalleExtracto.</p>
 *
 * <p><b>Corrección del 2026-08-27 (§7bis del diseño):</b> el ancla de tipo 1/2 era
 * {@code idMovimientoBanco}. Solo el 8% de los detalles de asiento sobre cuentas bancarias tiene
 * un TSR.MVCB asociado, así que el 92% no se podía declarar. {@code idDetalleAsiento} es lo que
 * siempre existe. {@code idMovimientoBanco} se conserva como dato informativo opcional — si se
 * envía, se valida contra el asiento del detalle, pero nunca es obligatorio.</p>
 *
 * <p>El valor NO se envía: lo calcula el backend a partir del registro referenciado, para que no
 * pueda descuadrar la ecuación un valor mal copiado desde el frontend.</p>
 */
public class PartidaTransitoSolicitud implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idDetalleAsiento;
    private Long idMovimientoBanco;
    private Long idDetalleExtracto;
    private Integer tipo;
    private String observacion;

    public Long getIdDetalleAsiento() { return idDetalleAsiento; }
    public void setIdDetalleAsiento(Long idDetalleAsiento) { this.idDetalleAsiento = idDetalleAsiento; }

    public Long getIdMovimientoBanco() { return idMovimientoBanco; }
    public void setIdMovimientoBanco(Long idMovimientoBanco) { this.idMovimientoBanco = idMovimientoBanco; }

    public Long getIdDetalleExtracto() { return idDetalleExtracto; }
    public void setIdDetalleExtracto(Long idDetalleExtracto) { this.idDetalleExtracto = idDetalleExtracto; }

    public Integer getTipo() { return tipo; }
    public void setTipo(Integer tipo) { this.tipo = tipo; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
