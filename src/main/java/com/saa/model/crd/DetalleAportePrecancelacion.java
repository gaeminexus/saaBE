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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.DAPR (DetalleAportePrecancelacion).
 *
 * Aportes del socio que se CONSUMEN (movimiento NEGATIVO, su saldo BAJA) para cubrir parte de
 * una precancelación registrada en {@code CRD.CBCR}. ⚠️ NO confundir con las líneas de aporte
 * de {@code COBRO_MIXTO} (movimiento POSITIVO, el socio ENTREGA plata y su saldo SUBE) — son
 * direcciones opuestas del dinero, por eso este desglose vive en su propia tabla y no reusa la
 * forma de línea de {@code DetalleCobroCredito}.
 *
 * {@code DetalleCobroCredito.valor} de la línea de PRECANCELACION es la parte de DEPÓSITO — la
 * única que genera cobro verificable por contabilidad. El total de la precancelación NUNCA se
 * guarda: se recalcula fresco con {@code simularPrecancelacion} en cada paso.
 *
 * Ver {@code docs/logica-negocio/crd/sql/85_PRECANCELACION_MIXTA_APORTES.sql}.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DAPR", schema = "CRD")
@SequenceGenerator(name = "SQ_DAPRCDGO", sequenceName = "CRD.SQ_DAPRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DetalleAportePrecancelacionAll", query = "select e from DetalleAportePrecancelacion e"),
    @NamedQuery(name = "DetalleAportePrecancelacionId",  query = "select e from DetalleAportePrecancelacion e where e.codigo = :id")
})
public class DetalleAportePrecancelacion implements Serializable {

    /** Código de la línea. PK. */
    @Id
    @Basic
    @Column(name = "DAPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DAPRCDGO")
    private Long codigo;

    /** FK - Línea del cobro (CRD.DCBC) a la que pertenece este consumo de aportes. */
    @ManyToOne
    @JoinColumn(name = "DCBCCDGO", referencedColumnName = "DCBCCDGO")
    private DetalleCobroCredito detalleCobroCredito;

    /** FK - Tipo de aporte que se consume. */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /** Cuánto se consume de ese tipo de aporte. El saldo se revalida DENTRO de la
     * transacción al PROCESAR, no al registrar — entre los dos momentos pasa la aprobación
     * de contabilidad y el socio pudo haber gastado ese saldo. */
    @Basic
    @Column(name = "DAPRVLOR")
    private Double valor;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public DetalleCobroCredito getDetalleCobroCredito() {
        return detalleCobroCredito;
    }

    public void setDetalleCobroCredito(DetalleCobroCredito detalleCobroCredito) {
        this.detalleCobroCredito = detalleCobroCredito;
    }

    public TipoAporte getTipoAporte() {
        return tipoAporte;
    }

    public void setTipoAporte(TipoAporte tipoAporte) {
        this.tipoAporte = tipoAporte;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }
}
