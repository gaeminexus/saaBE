package com.saa.model.rhh;

import java.io.Serializable;
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
 * Acumulado mensual por empleado y tipo de base. Alimenta decimos, fondos de reserva, vacaciones y la proyeccion de impuesto a la renta. Se escribe en cerrarPeriodo, nunca en calcularPeriodo.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ACMN", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "AcumuladoNominaId", query = "select e from AcumuladoNomina e where e.codigo=:id"),
    @NamedQuery(name = "AcumuladoNominaAll", query = "select e from AcumuladoNomina e")
})
public class AcumuladoNomina implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del acumulado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "ACMNCDGO")
    private Long codigo;

    /**
     * Empleado al que pertenece el acumulado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Periodo que lo genero; nulo en los saldos de apertura.
     */
    @ManyToOne
    @JoinColumn(name = "PRDNCDGO", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoNomina;

    /**
     * Anio al que corresponde el acumulado.
     */
    @Basic
    @Column(name = "ACMNANOO")
    private Integer anio;

    /**
     * Mes al que corresponde el acumulado.
     */
    @Basic
    @Column(name = "ACMNMSEE")
    private Integer mes;

    /**
     * Tipo de acumulado: detalle del rubro RHH_TIPO_ACUMULADO.
     */
    @Basic
    @Column(name = "ACMNTPAC")
    private Long tipoAcumulado;

    /**
     * Valor acumulado en el mes.
     */
    @Basic
    @Column(name = "ACMNVLOR")
    private Double valor;

    /**
     * Dias trabajados del mes.
     */
    @Basic
    @Column(name = "ACMNDIAS")
    private Double dias;

    /**
     * Proviene de un saldo de apertura de la migracion (S/N).
     */
    @Basic
    @Column(name = "ACMNAPRT", length = 1)
    private String aperturaMigracion;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "ACMNESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "ACMNFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "ACMNUSRR", length = 60)
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

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Long getTipoAcumulado() {
        return tipoAcumulado;
    }

    public void setTipoAcumulado(Long tipoAcumulado) {
        this.tipoAcumulado = tipoAcumulado;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getDias() {
        return dias;
    }

    public void setDias(Double dias) {
        this.dias = dias;
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
