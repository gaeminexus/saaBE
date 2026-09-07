package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;

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
import jakarta.persistence.Transient;

/**
 * Planilla que emite el portal del IESS por rubro y periodo: rol normal,
 * prestamos quirografarios, prestamos hipotecarios o fondos de reserva. Tabla:
 * RHH.PLIS.
 *
 * <p>No confundir con {@link PlanillaControlIess}, que es el POJO calculado
 * (sin tabla) contra el que esta se concilia. Ver
 * docs/logica-negocio/rhh/API-PLANILLA-IESS.md.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PLIS", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "PlanillaIessId", query = "select e from PlanillaIess e where e.codigo=:id"),
    @NamedQuery(name = "PlanillaIessAll", query = "select e from PlanillaIess e")
})
public class PlanillaIess implements Serializable {

    /**
     * Codigo unico de la planilla.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "PLISCDGO")
    private Long codigo;

    /**
     * Empresa a la que corresponde la planilla.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Periodo de nomina al que corresponde la planilla.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodo;

    /**
     * Tipo de planilla: detalle del rubro RHH_TIPO_PLANILLA_IESS (330).
     * Ver {@link com.saa.rubros.RhhTipoPlanillaIess}.
     */
    @Basic
    @Column(name = "PLISTIPO")
    private Long tipo;

    /**
     * Numero de comprobante que emite el portal. Unico por empresa y tipo.
     */
    @Basic
    @Column(name = "PLISNMCM", length = 50)
    private String numeroComprobante;

    /**
     * Fecha de emision del comprobante.
     */
    @Basic
    @Column(name = "PLISFCEM")
    private LocalDate fechaEmision;

    /**
     * Fecha maxima de pago (vencimiento del comprobante).
     */
    @Basic
    @Column(name = "PLISFCMX")
    private LocalDate fechaMaximaPago;

    /**
     * Total que cobra el IESS segun el comprobante del portal.
     */
    @Basic
    @Column(name = "PLISVLIS")
    private Double valorIess;

    /**
     * Nuestro total al conciliar. Snapshot: no se recalcula despues de
     * conciliar. Nulo si el tipo de planilla no tiene control calculado hoy
     * (solo el tipo 1, rol normal, lo tiene).
     */
    @Basic
    @Column(name = "PLISVLCT")
    private Double valorControl;

    /**
     * valorIess - valorControl. Se graba al conciliar, no se calcula al
     * vuelo: hay que poder auditar que diferencia se acepto.
     */
    @Basic
    @Column(name = "PLISDIFR")
    private Double diferencia;

    /**
     * Estado: 1 Registrada, 2 Conciliada, 3 Pagada, 4 Anulada. Ver
     * {@link com.saa.rubros.EstadoPlanillaIess}.
     */
    @Basic
    @Column(name = "PLISESTD")
    private Long estado;

    /**
     * Fecha real del debito del IESS. Se llena al pagar (Fase 2).
     */
    @Basic
    @Column(name = "PLISFCPG")
    private LocalDate fechaPago;

    /**
     * Asiento contable del pago. Se llena al pagar (Fase 2).
     */
    @ManyToOne
    @JoinColumn(name = "PLISASNT", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "PLISOBSR", length = 1000)
    private String observacion;

    /**
     * Motivo de anulacion.
     */
    @Basic
    @Column(name = "PLISMTAN", length = 500)
    private String motivoAnulacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "PLISFCRG")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "PLISUSAR")
    private Long usuario;

    /**
     * Renglones del comprobante (RHH.DLIS). No es columna de esta tabla: lo
     * puebla {@code PlanillaIessServiceImpl.selectById} en una consulta
     * aparte, igual que {@code MovimientoCajaChica.documentoTipo}.
     */
    @Transient
    private List<DetallePlanillaIess> renglones = new ArrayList<>();

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public PeriodoNomina getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoNomina periodo) {
        this.periodo = periodo;
    }

    public Long getTipo() {
        return tipo;
    }

    public void setTipo(Long tipo) {
        this.tipo = tipo;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaMaximaPago() {
        return fechaMaximaPago;
    }

    public void setFechaMaximaPago(LocalDate fechaMaximaPago) {
        this.fechaMaximaPago = fechaMaximaPago;
    }

    public Double getValorIess() {
        return valorIess;
    }

    public void setValorIess(Double valorIess) {
        this.valorIess = valorIess;
    }

    public Double getValorControl() {
        return valorControl;
    }

    public void setValorControl(Double valorControl) {
        this.valorControl = valorControl;
    }

    public Double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(Double diferencia) {
        this.diferencia = diferencia;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Asiento getAsiento() {
        return asiento;
    }

    public void setAsiento(Asiento asiento) {
        this.asiento = asiento;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Long getUsuario() {
        return usuario;
    }

    public void setUsuario(Long usuario) {
        this.usuario = usuario;
    }

    public List<DetallePlanillaIess> getRenglones() {
        return renglones;
    }

    public void setRenglones(List<DetallePlanillaIess> renglones) {
        this.renglones = renglones;
    }
}
