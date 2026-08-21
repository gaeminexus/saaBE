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
 * Proyeccion anual de gastos personales por categoria, presentada por el empleado. Alimenta la rebaja del impuesto a la renta.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "GSPR", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "GastoPersonalProyectadoId", query = "select e from GastoPersonalProyectado e where e.codigo=:id"),
    @NamedQuery(name = "GastoPersonalProyectadoAll", query = "select e from GastoPersonalProyectado e")
})
public class GastoPersonalProyectado implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la proyeccion.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "GSPRCDGO")
    private Long codigo;

    /**
     * Empleado que declara el gasto.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Anio fiscal de la proyeccion.
     */
    @Basic
    @Column(name = "GSPRANOO")
    private Integer anio;

    /**
     * Tipo de gasto: detalle del rubro RHH_TIPO_GASTO_PERSONAL.
     */
    @Basic
    @Column(name = "GSPRTPGP")
    private Long tipoGasto;

    /**
     * Valor proyectado del gasto.
     */
    @Basic
    @Column(name = "GSPRVLOR")
    private Double valor;

    /**
     * Fecha en que el empleado presento la proyeccion.
     */
    @Basic
    @Column(name = "GSPRFCPR")
    private LocalDate fechaPresentacion;

    /**
     * Es la version vigente de la proyeccion (S/N).
     */
    @Basic
    @Column(name = "GSPRVGNT", length = 1)
    private String vigente;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "GSPRESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "GSPRFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "GSPRUSRR", length = 60)
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

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Long getTipoGasto() {
        return tipoGasto;
    }

    public void setTipoGasto(Long tipoGasto) {
        this.tipoGasto = tipoGasto;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDate getFechaPresentacion() {
        return fechaPresentacion;
    }

    public void setFechaPresentacion(LocalDate fechaPresentacion) {
        this.fechaPresentacion = fechaPresentacion;
    }

    public String getVigente() {
        return vigente;
    }

    public void setVigente(String vigente) {
        this.vigente = vigente;
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
