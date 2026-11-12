package com.saa.model.rpr;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * C27-D (cuadre Superintendencia, 2026-11-11) — tabla RPR.CCPB: detalle del CCPM por rubro y
 * banda, una fila por operación, rubro y banda con valor distinto de cero.
 *
 * <p>El CCPM guarda una fila por préstamo con el capital por vencer en 5 columnas de banda y el
 * vencido en un solo valor. El Resumen Financiero ponía ese vencido entero en la banda de los
 * días de mora MÁXIMOS del préstamo, mientras que la contabilidad (y la Superintendencia) bandean
 * CADA cuota por sus días: el total por producto cuadraba y las bandas no. Aquí cada cuota queda
 * en su banda (por vencer y vencida) con la cuenta contable de la configuración de bandas del
 * producto, igual que el cierre de cartera; y los demás rubros (interés vencido, mora,
 * desgravamen, seguro de incendio, provisión) quedan por operación y tipo de préstamo para
 * compararlos con su cuenta.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CCPB", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "CreditoCuotasPrestamosBandaAll", query = "select e from CreditoCuotasPrestamosBanda e"),
    @NamedQuery(name = "CreditoCuotasPrestamosBandaId",  query = "select e from CreditoCuotasPrestamosBanda e where e.codigo = :id")
})
public class CreditoCuotasPrestamosBanda implements Serializable {

    /** Capital de cuotas por vencer al corte, bandeado por cuota. */
    public static final String RUBRO_CAPITAL_POR_VENCER = "CAPITAL_POR_VENCER";
    /** Capital de cuotas vencidas al corte, bandeado por cuota (días de ESA cuota). */
    public static final String RUBRO_CAPITAL_VENCIDO    = "CAPITAL_VENCIDO";
    /** Interés pendiente de las cuotas vencidas al corte (el cobrable del ③ del cierre). */
    public static final String RUBRO_INTERES_VENCIDO    = "INTERES_VENCIDO";
    /** Mora calculada al corte de las cuotas vencidas (informativa: no tiene cuenta). */
    public static final String RUBRO_MORA               = "MORA";
    /** Desgravamen pendiente de las cuotas vencidas al corte. */
    public static final String RUBRO_DESGRAVAMEN        = "DESGRAVAMEN";
    /** Seguro de incendio pendiente de las cuotas vencidas al corte. */
    public static final String RUBRO_SEGURO_INCENDIO    = "SEGURO_INCENDIO";
    /** Provisión requerida de la operación (la de la G48 al corte). */
    public static final String RUBRO_PROVISION          = "PROVISION";

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CCPBCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Ejecución de reportes de cartera (RPR.EJCC). */
    @Basic
    @Column(name = "CCPBEJCC")
    private Long idEjecucion;

    /** Número de operación (PRST.PRSTIDAS), el mismo de CCPM.CCPMNMOP. */
    @Basic
    @Column(name = "CCPBNMOP", length = 50)
    private String numeroOperacion;

    /** Préstamo (CRD.PRST). */
    @Basic
    @Column(name = "CCPBPRST")
    private Long idPrestamo;

    /** Producto (CRD.PRDC). */
    @Basic
    @Column(name = "CCPBPRDC")
    private Long idProducto;

    /** Tipo de préstamo (CRD.TPPR). */
    @Basic
    @Column(name = "CCPBTPPR")
    private Long idTipoPrestamo;

    /** Rubro (constantes RUBRO_*). */
    @Basic
    @Column(name = "CCPBRBRO", length = 30)
    private String rubro;

    /** Número de banda (1..N) en los rubros de capital; nulo en los demás. */
    @Basic
    @Column(name = "CCPBNMBN")
    private Long numeroBanda;

    /** Etiqueta de la banda ("1 - 30", "más de 360 (resto)"). */
    @Basic
    @Column(name = "CCPBETQT", length = 100)
    private String etiquetaBanda;

    /** Cuenta contable con puntos (de la banda); nula en los rubros sin banda. */
    @Basic
    @Column(name = "CCPBCNTA", length = 50)
    private String cuentaContable;

    /** Valor del rubro en la banda. */
    @Basic
    @Column(name = "CCPBVLOR")
    private Double valor;

    public CreditoCuotasPrestamosBanda() {
    }

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Long getIdEjecucion() { return idEjecucion; }
    public void setIdEjecucion(Long idEjecucion) { this.idEjecucion = idEjecucion; }

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String numeroOperacion) { this.numeroOperacion = numeroOperacion; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }

    public Long getIdTipoPrestamo() { return idTipoPrestamo; }
    public void setIdTipoPrestamo(Long idTipoPrestamo) { this.idTipoPrestamo = idTipoPrestamo; }

    public String getRubro() { return rubro; }
    public void setRubro(String rubro) { this.rubro = rubro; }

    public Long getNumeroBanda() { return numeroBanda; }
    public void setNumeroBanda(Long numeroBanda) { this.numeroBanda = numeroBanda; }

    public String getEtiquetaBanda() { return etiquetaBanda; }
    public void setEtiquetaBanda(String etiquetaBanda) { this.etiquetaBanda = etiquetaBanda; }

    public String getCuentaContable() { return cuentaContable; }
    public void setCuentaContable(String cuentaContable) { this.cuentaContable = cuentaContable; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
}
