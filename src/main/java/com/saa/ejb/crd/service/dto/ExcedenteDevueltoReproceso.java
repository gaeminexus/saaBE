package com.saa.ejb.crd.service.dto;

/**
 * Un excedente Petro que ASOPREP DEVOLVIÓ al partícipe — decisión D-010 del cuadre
 * Superintendencia (cambio C4, {@code docs/logica-negocio/petro/REPROCESO-CARGA-PETRO.md} §4.7).
 *
 * <p>Viaja en {@link SolicitudReprocesoCargaPetro#getExcedentesDevueltos()}. En modo reproceso,
 * el monto indicado NO entra a la cascada de cuotas siguientes (préstamos) ni se anticipa a
 * meses siguientes (aportes AH): se registra como aporte {@code EXCEDENTE_PETRO} del partícipe
 * con la fecha del mes de la carga, y la devolución se registra después, aparte, con la fecha
 * real en que se le pagó. Lo que no figura en la lista sigue el camino normal del motor.</p>
 *
 * <p>Identificación de la fila de la carga: {@code codigoPetro} (rol Petro, {@code PXCACDPT}) o
 * {@code idEntidad} (código interno del partícipe, {@code ENTDCDGO}, se traduce a su rol), más
 * {@code codigoProducto} tal como viene en el archivo ({@code PE}, {@code PH}, {@code PQ},
 * {@code PP}, {@code AH}). Para PH/PP el monto puede incluir el HS del mismo partícipe.</p>
 */
public class ExcedenteDevueltoReproceso {

    private Long codigoPetro;
    private Long idEntidad;
    private String codigoProducto;
    private Double monto;
    /** Opcional: 11 cesantía / 9 jubilación. Si falta, cesantía si tiene vigencia en el mes; si no, jubilación. */
    private Long idTipoAporte;
    /** Documento que respalda la devolución (comprobante, correo, acta de D-010). Obligatorio. */
    private String referencia;

    public Long getCodigoPetro() {
        return codigoPetro;
    }

    public void setCodigoPetro(Long codigoPetro) {
        this.codigoPetro = codigoPetro;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}
