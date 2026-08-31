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
 * Representa la tabla CRD.DAAP (DetalleAporteAcuerdoCondonacion).
 *
 * Desglose por tipo de aporte del cruce que cubre la parte {@code AcuerdoCondonacion.valorPagarAportes}
 * de un acuerdo de pago con condonación. Sin esto solo quedaría el total — se perdería DE
 * QUÉ tipos de aporte y cuánto de cada uno, justo entre el registro y el proceso, que ocurren
 * en momentos distintos (el segundo puede esperar días a la aprobación del depósito cuando
 * también hay parte {@code valorPagarDeposito}).
 *
 * Mismo desglose que ya viaja en {@code SolicitudPagoConAportes.aportes}/
 * {@code SolicitudPrecancelacion.aportes} — un tipo de aporte no puede repetirse en el mismo
 * acuerdo (UK_DAAP_ACCN_TPAP), misma regla que ya aplica
 * {@code ProcesoPagoPrestamoService#validarDesgloseAportes}.
 *
 * Ver {@code docs/logica-negocio/crd/sql/84_ACUERDO_PAGO_CON_APORTES.sql} y
 * {@code docs/logica-negocio/crd/PLAN-ACUERDOS-PAGO-CONDONACION.md}.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DAAP", schema = "CRD")
@SequenceGenerator(name = "SQ_DAAPCDGO", sequenceName = "CRD.SQ_DAAPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DetalleAporteAcuerdoCondonacionAll", query = "select e from DetalleAporteAcuerdoCondonacion e"),
    @NamedQuery(name = "DetalleAporteAcuerdoCondonacionId",  query = "select e from DetalleAporteAcuerdoCondonacion e where e.codigo = :id")
})
public class DetalleAporteAcuerdoCondonacion implements Serializable {

    /** Código de la línea. PK. */
    @Id
    @Basic
    @Column(name = "DAAPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DAAPCDGO")
    private Long codigo;

    /** FK - Acuerdo al que pertenece esta línea. */
    @ManyToOne
    @JoinColumn(name = "ACCNCDGO", referencedColumnName = "ACCNCDGO")
    private AcuerdoCondonacion acuerdo;

    /** FK - Tipo de aporte que se consume. */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /** Cuánto se consume de ese tipo de aporte. El saldo se revalida DENTRO de la
     * transacción al procesar ({@code consumirAportes}), no al registrar: entre los dos
     * momentos el saldo pudo cambiar. */
    @Basic
    @Column(name = "DAAPVLOR")
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

    public AcuerdoCondonacion getAcuerdo() {
        return acuerdo;
    }

    public void setAcuerdo(AcuerdoCondonacion acuerdo) {
        this.acuerdo = acuerdo;
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
