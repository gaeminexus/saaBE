package com.saa.model.crd;

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
 * Representa la tabla DDVA (DetalleDevolucionAporte).
 *
 * Detalle de una devolución de aportes por tipo de aporte. Cada fila deja la traza de la
 * fila NEGATIVA de CRD.APRT y del CRD.PGAP que generó, y —si el pago termina rechazado o
 * reversado— de la fila POSITIVA de contra-movimiento.
 *
 * <b>Un reverso NUNCA borra ni edita la fila negativa</b>: inserta una positiva. CRD.APRT
 * es append-only para los reportes (G42, G43, G44, CJBM, CPRM/CCPM, dashboard, padrón), y
 * el G43 en particular liquida cesantes leyendo explícitamente los negativos del mes.
 *
 * Los tres códigos de traza ({@code idAporte}, {@code idPagoAporte}, {@code idAporteReverso})
 * son números sin FK: apuntan a filas append-only cuya vida no la controla esta tabla.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DDVA", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "DetalleDevolucionAporteAll",
        query = "select e from DetalleDevolucionAporte e"),
    @NamedQuery(name = "DetalleDevolucionAporteId",
        query = "select e from DetalleDevolucionAporte e where e.codigo = :id")
})
public class DetalleDevolucionAporte implements Serializable {

    /** Código del detalle. */
    @Id
    @Basic
    @Column(name = "DDVACDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Devolución a la que pertenece el detalle. */
    @ManyToOne
    @JoinColumn(name = "DVAPCDGO", referencedColumnName = "DVAPCDGO")
    private DevolucionAporte devolucion;

    /** FK - Tipo de aporte devuelto. */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /** Valor devuelto de este tipo de aporte, en positivo. */
    @Basic
    @Column(name = "DDVAVLRR")
    private Double valor;

    /** CRD.APRT.APRTCDGO de la fila NEGATIVA generada al registrar. Sin FK. */
    @Basic
    @Column(name = "DDVAAPRT")
    private Long idAporte;

    /** CRD.PGAP.PGAPCDGO generado junto al aporte negativo. Sin FK. */
    @Basic
    @Column(name = "DDVAPGAP")
    private Long idPagoAporte;

    /**
     * CRD.APRT.APRTCDGO de la fila POSITIVA de contra-movimiento, generada si el pago se
     * rechaza o se reversa. NULL mientras no ocurra: es la marca de idempotencia del
     * reconciliador. Sin FK.
     */
    @Basic
    @Column(name = "DDVAAPRV")
    private Long idAporteReverso;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public DevolucionAporte getDevolucion() { return devolucion; }
    public void setDevolucion(DevolucionAporte devolucion) { this.devolucion = devolucion; }

    public TipoAporte getTipoAporte() { return tipoAporte; }
    public void setTipoAporte(TipoAporte tipoAporte) { this.tipoAporte = tipoAporte; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public Long getIdAporte() { return idAporte; }
    public void setIdAporte(Long idAporte) { this.idAporte = idAporte; }

    public Long getIdPagoAporte() { return idPagoAporte; }
    public void setIdPagoAporte(Long idPagoAporte) { this.idPagoAporte = idPagoAporte; }

    public Long getIdAporteReverso() { return idAporteReverso; }
    public void setIdAporteReverso(Long idAporteReverso) { this.idAporteReverso = idAporteReverso; }
}
