package com.saa.model.rhh;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Qué {@link CuotaDescuento} tocó una {@link DevolucionAnticipo} y cómo. Tabla:
 * RHH.DVCT.
 *
 * <p>Sin esto, anular una devolución no podría saber con certeza qué cuotas
 * devolver a PENDIENTE cuando el anticipo tuvo más de una devolución
 * (decisión del usuario, ver
 * docs/logica-negocio/rhh/API-DEVOLUCION-ANTICIPO.md #1) — reactivaría la
 * equivocada, y no se notaría hasta que a alguien le descuenten un mes que ya
 * había devuelto. Además responde «¿por qué esta cuota no se descontó?», que
 * es la pregunta cuando un empleado reclama (#2.2).</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DVCT", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DevolucionCuotaAnticipoId", query = "select e from DevolucionCuotaAnticipo e where e.codigo=:id"),
    @NamedQuery(name = "DevolucionCuotaAnticipoAll", query = "select e from DevolucionCuotaAnticipo e")
})
public class DevolucionCuotaAnticipo implements Serializable {

    /** Cancela la cuota entera (pasa a ANULADA). */
    public static final int CANCELADA = 1;

    /** Baja el valor de la cuota, que sigue PENDIENTE. */
    public static final int AJUSTADA = 2;

    /**
     * Codigo unico de la fila.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DVCTCDGO")
    private Long codigo;

    /**
     * Devolución que tocó la cuota.
     */
    @ManyToOne
    @JoinColumn(name = "DVANCDGO", referencedColumnName = "DVANCDGO")
    private DevolucionAnticipo devolucion;

    /**
     * Cuota tocada.
     */
    @ManyToOne
    @JoinColumn(name = "CTDSCDGO", referencedColumnName = "CTDSCDGO")
    private CuotaDescuento cuota;

    /**
     * Tipo de efecto: {@link #CANCELADA} o {@link #AJUSTADA}.
     */
    @Basic
    @Column(name = "DVCTTIPO")
    private Long tipo;

    /**
     * Valor de la devolución aplicado a esta cuota.
     */
    @Basic
    @Column(name = "DVCTVLAP")
    private Double valorAplicado;

    /**
     * Valor que tenía la cuota antes de esta devolución. Nulo cuando
     * {@link #tipo} es {@link #CANCELADA} (cancelar no cambia el valor, sólo
     * el estado).
     */
    @Basic
    @Column(name = "DVCTVLAN")
    private Double valorAnterior;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public DevolucionAnticipo getDevolucion() {
        return devolucion;
    }

    public void setDevolucion(DevolucionAnticipo devolucion) {
        this.devolucion = devolucion;
    }

    public CuotaDescuento getCuota() {
        return cuota;
    }

    public void setCuota(CuotaDescuento cuota) {
        this.cuota = cuota;
    }

    public Long getTipo() {
        return tipo;
    }

    public void setTipo(Long tipo) {
        this.tipo = tipo;
    }

    public Double getValorAplicado() {
        return valorAplicado;
    }

    public void setValorAplicado(Double valorAplicado) {
        this.valorAplicado = valorAplicado;
    }

    public Double getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(Double valorAnterior) {
        this.valorAnterior = valorAnterior;
    }
}
