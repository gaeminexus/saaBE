package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
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

/**
 * Reparto de utilidades de un ejercicio.
 *
 * <p><b>Las tres bases se llaman por lo que reparten, no por su porcentaje.</b> Los sufijos
 * numericos de las columnas —<code>UTLDBS15</code>, <code>UTLDBS10</code>,
 * <code>UTLDBS05</code>— son los porcentajes de ley, pero esos porcentajes viven en
 * <code>PRNMUTPR</code>, <code>PRNMUTDI</code> y <code>PRNMUTCG</code>: si el legislador los
 * cambia, una propiedad llamada <code>base15</code> pasa a mentir.</p>
 *
 * <p><b>Ojo con <code>valorPorDia</code> y <code>valorPorCarga</code>:</b> son los
 * <b>coeficientes</b> de la empresa. Los importes del empleado estan en
 * <code>DetalleUtilidad.valorPorDias</code> y <code>valorPorCargas</code>, en plural. Se
 * parecen a proposito —expresan la misma reparticion en dos niveles— pero no son lo mismo.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "UTLD", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "UtilidadId",  query = "select e from Utilidad e where e.codigo=:id"),
    @NamedQuery(name = "UtilidadAll", query = "select e from Utilidad e")
})
public class Utilidad implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo del reparto.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "UTLDCDGO")
    private Long codigo;

    /**
     * Empresa.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Ejercicio fiscal. Unico por empresa.
     */
    @Basic
    @Column(name = "UTLDANOO")
    private Integer anio;

    /**
     * Utilidad contable del ejercicio. La informa el usuario.
     */
    @Basic
    @Column(name = "UTLDUTCN")
    private Double utilidadContable;

    /**
     * Base total a repartir entre los trabajadores.
     */
    @Basic
    @Column(name = "UTLDBS15")
    private Double baseTotal;

    /**
     * Parte de la base que se reparte por dias trabajados.
     */
    @Basic
    @Column(name = "UTLDBS10")
    private Double basePorDias;

    /**
     * Parte de la base que se reparte por cargas familiares.
     */
    @Basic
    @Column(name = "UTLDBS05")
    private Double basePorCargas;

    /**
     * Total de dias trabajados de la empresa en el ejercicio.
     */
    @Basic
    @Column(name = "UTLDTTDI")
    private Double totalDias;

    /**
     * Total de cargas familiares de la empresa.
     */
    @Basic
    @Column(name = "UTLDTTCG")
    private Integer totalCargas;

    /**
     * Coeficiente por dia trabajado.
     */
    @Basic
    @Column(name = "UTLDVLDI")
    private Double valorPorDia;

    /**
     * Coeficiente por carga familiar.
     */
    @Basic
    @Column(name = "UTLDVLCG")
    private Double valorPorCarga;

    /**
     * Tope por trabajador, en dolares.
     */
    @Basic
    @Column(name = "UTLDTPSB")
    private Double topePorTrabajador;

    /**
     * Excedente total transferido al IESS.
     */
    @Basic
    @Column(name = "UTLDEXCD")
    private Double excedente;

    /**
     * Fecha de pago.
     */
    @Basic
    @Column(name = "UTLDFCPG")
    private LocalDate fechaPago;

    /**
     * Periodo de nomina por el que se pago.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoNomina;

    /**
     * Estado del reparto.
     */
    @Basic
    @Column(name = "UTLDESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "UTLDFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "UTLDUSRR", length = 60)
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

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

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Double getUtilidadContable() {
        return utilidadContable;
    }

    public void setUtilidadContable(Double utilidadContable) {
        this.utilidadContable = utilidadContable;
    }

    public Double getBaseTotal() {
        return baseTotal;
    }

    public void setBaseTotal(Double baseTotal) {
        this.baseTotal = baseTotal;
    }

    public Double getBasePorDias() {
        return basePorDias;
    }

    public void setBasePorDias(Double basePorDias) {
        this.basePorDias = basePorDias;
    }

    public Double getBasePorCargas() {
        return basePorCargas;
    }

    public void setBasePorCargas(Double basePorCargas) {
        this.basePorCargas = basePorCargas;
    }

    public Double getTotalDias() {
        return totalDias;
    }

    public void setTotalDias(Double totalDias) {
        this.totalDias = totalDias;
    }

    public Integer getTotalCargas() {
        return totalCargas;
    }

    public void setTotalCargas(Integer totalCargas) {
        this.totalCargas = totalCargas;
    }

    public Double getValorPorDia() {
        return valorPorDia;
    }

    public void setValorPorDia(Double valorPorDia) {
        this.valorPorDia = valorPorDia;
    }

    public Double getValorPorCarga() {
        return valorPorCarga;
    }

    public void setValorPorCarga(Double valorPorCarga) {
        this.valorPorCarga = valorPorCarga;
    }

    public Double getTopePorTrabajador() {
        return topePorTrabajador;
    }

    public void setTopePorTrabajador(Double topePorTrabajador) {
        this.topePorTrabajador = topePorTrabajador;
    }

    public Double getExcedente() {
        return excedente;
    }

    public void setExcedente(Double excedente) {
        this.excedente = excedente;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
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
