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
 * Decimo tercero, decimo cuarto, fondos de reserva, vacaciones o utilidades liquidados a un empleado en un anio.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "LQBS", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "LiquidacionBeneficioSocialId", query = "select e from LiquidacionBeneficioSocial e where e.codigo=:id"),
    @NamedQuery(name = "LiquidacionBeneficioSocialAll", query = "select e from LiquidacionBeneficioSocial e")
})
public class LiquidacionBeneficioSocial implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del beneficio liquidado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "LQBSCDGO")
    private Long codigo;

    /**
     * Empleado beneficiario.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Tipo de beneficio: detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL.
     */
    @Basic
    @Column(name = "LQBSTPBN")
    private Long tipoBeneficio;

    /**
     * Anio del beneficio.
     */
    @Basic
    @Column(name = "LQBSANOO")
    private Integer anio;

    /**
     * Inicio del periodo del beneficio.
     */
    @Basic
    @Column(name = "LQBSFCHI")
    private LocalDate fechaInicio;

    /**
     * Fin del periodo del beneficio.
     */
    @Basic
    @Column(name = "LQBSFCHF")
    private LocalDate fechaFin;

    /**
     * Base acumulada del beneficio.
     */
    @Basic
    @Column(name = "LQBSBSCL")
    private Double baseCalculo;

    /**
     * Dias trabajados en el periodo.
     */
    @Basic
    @Column(name = "LQBSDIAS")
    private Double dias;

    /**
     * Valor calculado del beneficio.
     */
    @Basic
    @Column(name = "LQBSVLRR")
    private Double valor;

    /**
     * Valor ya pagado en forma mensualizada, que se resta del calculado.
     */
    @Basic
    @Column(name = "LQBSVLMN")
    private Double valorMensualizado;

    /**
     * Valor efectivamente pagado.
     */
    @Basic
    @Column(name = "LQBSVLPG")
    private Double valorPagado;

    /**
     * Periodo de nomina por el que se pago.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoNomina;

    /**
     * Fecha de pago del beneficio.
     */
    @Basic
    @Column(name = "LQBSFCPG")
    private LocalDate fechaPago;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "LQBSESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "LQBSFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "LQBSUSRR", length = 60)
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

    public Long getTipoBeneficio() {
        return tipoBeneficio;
    }

    public void setTipoBeneficio(Long tipoBeneficio) {
        this.tipoBeneficio = tipoBeneficio;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
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

    public Double getBaseCalculo() {
        return baseCalculo;
    }

    public void setBaseCalculo(Double baseCalculo) {
        this.baseCalculo = baseCalculo;
    }

    public Double getDias() {
        return dias;
    }

    public void setDias(Double dias) {
        this.dias = dias;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getValorMensualizado() {
        return valorMensualizado;
    }

    public void setValorMensualizado(Double valorMensualizado) {
        this.valorMensualizado = valorMensualizado;
    }

    public Double getValorPagado() {
        return valorPagado;
    }

    public void setValorPagado(Double valorPagado) {
        this.valorPagado = valorPagado;
    }

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
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
