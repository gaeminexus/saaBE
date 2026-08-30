package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Representa la tabla CRD.DCBC (DetalleCobroCredito).
 *
 * Detalle de un {@link CobroCredito}: una fila por préstamo. El cobro múltiple es UN
 * CobroCredito con N filas acá. REGISTRO_APORTE lleva una sola fila con {@code prestamo}
 * nulo (el aporte es de la entidad, no de un préstamo).
 *
 * Las tres columnas específicas por tipo de operación (modalidad, tipo de aporte, período
 * de devengo) son nullable A PROPÓSITO: se llenan solo cuando el tipo de operación las
 * necesita. Ver {@code docs/logica-negocio/crd/sql/DDL-COBROS-APROBACION-CONTABILIDAD.sql}.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DCBC", schema = "CRD")
@SequenceGenerator(name = "SQ_DCBCCDGO", sequenceName = "CRD.SQ_DCBCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DetalleCobroCreditoAll", query = "select e from DetalleCobroCredito e"),
    @NamedQuery(name = "DetalleCobroCreditoId",  query = "select e from DetalleCobroCredito e where e.codigo = :id")
})
public class DetalleCobroCredito implements Serializable {

    /** Código de la línea. PK. */
    @Id
    @Basic
    @Column(name = "DCBCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DCBCCDGO")
    private Long codigo;

    /** FK - Cobro al que pertenece esta línea. */
    @ManyToOne
    @JoinColumn(name = "CBCRCDGO", referencedColumnName = "CBCRCDGO")
    private CobroCredito cobroCredito;

    /** FK - Préstamo afectado. NULL solo en REGISTRO_APORTE. */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** Monto de esta línea. */
    @Basic
    @Column(name = "DCBCVLRR")
    private Double valor;

    /** Modalidad del abono a capital (1 = reduce plazo, 2 = reduce cuota). NULL en los demás tipos de operación. */
    @Basic
    @Column(name = "DCBCMDLD")
    private Long modalidad;

    /** FK - Tipo de aporte. Solo REGISTRO_APORTE. */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /** Período de devengo. Solo REGISTRO_APORTE. */
    @Basic
    @Column(name = "DCBCPRDV")
    private LocalDate periodoDevengo;

    /** FK - EventoPrestamo generado AL PROCESAR esta línea. NULL mientras el cobro no se haya procesado. */
    @ManyToOne
    @JoinColumn(name = "EVPRCDGO", referencedColumnName = "EVPRCDGO")
    private EventoPrestamo eventoPrestamo;

    /** FK - PagoAporte generado al procesar (REGISTRO_APORTE). */
    @ManyToOne
    @JoinColumn(name = "PGAPCDGO", referencedColumnName = "PGAPCDGO")
    private PagoAporte pagoAporte;

    /** Observación de esta línea. */
    @Basic
    @Column(name = "DCBCOBSR", length = 2000)
    private String observacion;

    /** FK - Acuerdo de condonación cuando el cobro es de tipo ACUERDO_CONDONACION. NULL en los demás tipos. */
    @ManyToOne
    @JoinColumn(name = "ACCNCDGO", referencedColumnName = "ACCNCDGO")
    private AcuerdoCondonacion acuerdoCondonacion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public CobroCredito getCobroCredito() {
        return cobroCredito;
    }

    public void setCobroCredito(CobroCredito cobroCredito) {
        this.cobroCredito = cobroCredito;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getModalidad() {
        return modalidad;
    }

    public void setModalidad(Long modalidad) {
        this.modalidad = modalidad;
    }

    public TipoAporte getTipoAporte() {
        return tipoAporte;
    }

    public void setTipoAporte(TipoAporte tipoAporte) {
        this.tipoAporte = tipoAporte;
    }

    public LocalDate getPeriodoDevengo() {
        return periodoDevengo;
    }

    public void setPeriodoDevengo(LocalDate periodoDevengo) {
        this.periodoDevengo = periodoDevengo;
    }

    public EventoPrestamo getEventoPrestamo() {
        return eventoPrestamo;
    }

    public void setEventoPrestamo(EventoPrestamo eventoPrestamo) {
        this.eventoPrestamo = eventoPrestamo;
    }

    public PagoAporte getPagoAporte() {
        return pagoAporte;
    }

    public void setPagoAporte(PagoAporte pagoAporte) {
        this.pagoAporte = pagoAporte;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public AcuerdoCondonacion getAcuerdoCondonacion() {
        return acuerdoCondonacion;
    }

    public void setAcuerdoCondonacion(AcuerdoCondonacion acuerdoCondonacion) {
        this.acuerdoCondonacion = acuerdoCondonacion;
    }
}
