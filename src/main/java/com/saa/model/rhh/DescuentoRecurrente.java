package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;

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
 * Obligacion que se descuenta al empleado mes a mes: prestamos IESS, anticipos, prestamos internos, retenciones judiciales y seguros privados.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DSRC", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DescuentoRecurrenteId", query = "select e from DescuentoRecurrente e where e.codigo=:id"),
    @NamedQuery(name = "DescuentoRecurrenteAll", query = "select e from DescuentoRecurrente e")
})
public class DescuentoRecurrente implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del descuento.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DSRCCDGO")
    private Long codigo;

    /**
     * Empleado al que se le descuenta.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Concepto de nomina con el que se aplica el descuento.
     */
    @ManyToOne
    @JoinColumn(name = "CPNMCDGO", referencedColumnName = "CPNMCDGO")
    private ConceptoNomina conceptoNomina;

    /**
     * Tipo de descuento: detalle del rubro RHH_TIPO_DESCUENTO_RECURRENTE.
     */
    @Basic
    @Column(name = "DSRCTPDS")
    private Long tipoDescuento;

    /**
     * Numero de referencia del prestamo u obligacion.
     */
    @Basic
    @Column(name = "DSRCNMRO", length = 50)
    private String numero;

    /**
     * Monto original de la obligacion.
     */
    @Basic
    @Column(name = "DSRCVLOR")
    private Double valor;

    /**
     * Saldo pendiente por descontar.
     */
    @Basic
    @Column(name = "DSRCSLDD")
    private Double saldo;

    /**
     * Numero total de cuotas.
     */
    @Basic
    @Column(name = "DSRCNMCT")
    private Integer numeroCuotas;

    /**
     * Numero de cuotas ya descontadas.
     */
    @Basic
    @Column(name = "DSRCCTPG")
    private Integer cuotasPagadas;

    /**
     * Valor de la cuota mensual.
     */
    @Basic
    @Column(name = "DSRCVLCT")
    private Double valorCuota;

    /**
     * Porcentaje sobre el neto, en retenciones judiciales.
     */
    @Basic
    @Column(name = "DSRCPRCN")
    private Double porcentaje;

    /**
     * Fecha de inicio del descuento.
     */
    @Basic
    @Column(name = "DSRCFCHI")
    private LocalDate fechaInicio;

    /**
     * Fecha estimada de finalizacion.
     */
    @Basic
    @Column(name = "DSRCFCHF")
    private LocalDate fechaFin;

    /**
     * Beneficiario del descuento.
     */
    @Basic
    @Column(name = "DSRCBNFC", length = 200)
    private String beneficiario;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "DSRCOBSR", length = 500)
    private String observacion;

    /**
     * Proviene de un saldo de apertura de la migracion (S/N).
     */
    @Basic
    @Column(name = "DSRCAPRT", length = 1)
    private String aperturaMigracion;

    /**
     * Estado: detalle del rubro RHH_ESTADO_DESCUENTO_RECURRENTE.
     */
    @Basic
    @Column(name = "DSRCESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DSRCFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "DSRCUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public ConceptoNomina getConceptoNomina() {
        return conceptoNomina;
    }

    public void setConceptoNomina(ConceptoNomina conceptoNomina) {
        this.conceptoNomina = conceptoNomina;
    }

    public Long getTipoDescuento() {
        return tipoDescuento;
    }

    public void setTipoDescuento(Long tipoDescuento) {
        this.tipoDescuento = tipoDescuento;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public Integer getNumeroCuotas() {
        return numeroCuotas;
    }

    public void setNumeroCuotas(Integer numeroCuotas) {
        this.numeroCuotas = numeroCuotas;
    }

    public Integer getCuotasPagadas() {
        return cuotasPagadas;
    }

    public void setCuotasPagadas(Integer cuotasPagadas) {
        this.cuotasPagadas = cuotasPagadas;
    }

    public Double getValorCuota() {
        return valorCuota;
    }

    public void setValorCuota(Double valorCuota) {
        this.valorCuota = valorCuota;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getBeneficiario() {
        return beneficiario;
    }

    public void setBeneficiario(String beneficiario) {
        this.beneficiario = beneficiario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getAperturaMigracion() {
        return aperturaMigracion;
    }

    public void setAperturaMigracion(String aperturaMigracion) {
        this.aperturaMigracion = aperturaMigracion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
