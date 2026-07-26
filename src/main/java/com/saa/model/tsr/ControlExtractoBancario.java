/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial").
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Periodo;
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
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.CTEB.
 * Entity ControlExtractoBancario.
 * Resumen mensual de cumplimiento de carga y conciliacion de extractos
 * bancarios, por empresa. Una fila por periodo con los totales; el
 * detalle de que cuenta especifica falta o ya cumplio se obtiene
 * consultando CuentaBancaria/ExtractoBancario directamente.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CTEB", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "ControlExtractoBancarioAll",
        query = "select e from ControlExtractoBancario e order by e.anio desc, e.mes desc"),
    @NamedQuery(name = "ControlExtractoBancarioId",
        query = "select e from ControlExtractoBancario e where e.codigo = :id"),
    @NamedQuery(name = "ControlExtractoBancarioByEmpresaYPeriodo",
        query = "select e from ControlExtractoBancario e "
              + "where e.empresa.codigo = :idEmpresa and e.mes = :mes and e.anio = :anio")
})
public class ControlExtractoBancario implements Serializable {

    /**
     * Código / PK autoincrementable.
     */
    @Basic
    @Id
    @Column(name = "CTEBCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK a la Empresa.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", nullable = false)
    private Empresa empresa;

    /**
     * FK al Periodo contable correspondiente.
     */
    @ManyToOne
    @JoinColumn(name = "PRDOCDGO", nullable = false)
    private Periodo periodo;

    /**
     * Mes del periodo (1-12), denormalizado del Periodo para filtrar sin join.
     */
    @Basic
    @Column(name = "CTEBMSSS")
    private Long mes;

    /**
     * Año del periodo, denormalizado del Periodo.
     */
    @Basic
    @Column(name = "CTEBANOO")
    private Long anio;

    /**
     * Fecha limite general para tener todos los extractos cargados este mes.
     */
    @Basic
    @Column(name = "CTEBFVNC")
    private LocalDate fechaVencimiento;

    /**
     * Total de cuentas bancarias activas a conciliar este periodo.
     * Se fija al momento de generar el registro (ver ControlExtractoBancarioServiceImpl.generarPeriodo),
     * no se recalcula despues - una cuenta abierta a mitad de mes no infla el denominador
     * de un periodo del que no formaba parte al inicio.
     */
    @Basic
    @Column(name = "CTEBTOTC")
    private Long totalCuentas;

    /**
     * Cuantas de esas cuentas ya tienen al menos un extracto que cubre el mes completo.
     */
    @Basic
    @Column(name = "CTEBCARG")
    private Long cuentasCargadas;

    /**
     * Cuantas de esas cuentas ya completaron el proceso de conciliacion para este periodo.
     */
    @Basic
    @Column(name = "CTEBCONC")
    private Long cuentasConciliadas;

    /**
     * Notas generales del periodo (ej. "banco X en mantenimiento, extracto tardio para todos sus clientes").
     */
    @Basic
    @Column(name = "CTEBOBSR", length = 1000)
    private String observaciones;

    /**
     * Fecha y hora en que se genero/actualizo por ultima vez este resumen.
     * Es un snapshot, no un valor "vivo" - un reporte deberia mostrar
     * "a fecha de [fechaCreacion]" en vez de asumir que esta siempre al dia.
     */
    @Basic
    @Column(name = "CTEBFCRG")
    private LocalDateTime fechaCreacion;

    /**
     * Estado del registro. 1 = Activo, 0 = Inactivo.
     */
    @Basic
    @Column(name = "CTEBESTD")
    private Long estado;

    // -------------------------------------------------------------------------
    // GETTERS Y SETTERS
    // -------------------------------------------------------------------------

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

    public Periodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Periodo periodo) {
        this.periodo = periodo;
    }

    public Long getMes() {
        return mes;
    }

    public void setMes(Long mes) {
        this.mes = mes;
    }

    public Long getAnio() {
        return anio;
    }

    public void setAnio(Long anio) {
        this.anio = anio;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Long getTotalCuentas() {
        return totalCuentas;
    }

    public void setTotalCuentas(Long totalCuentas) {
        this.totalCuentas = totalCuentas;
    }

    public Long getCuentasCargadas() {
        return cuentasCargadas;
    }

    public void setCuentasCargadas(Long cuentasCargadas) {
        this.cuentasCargadas = cuentasCargadas;
    }

    public Long getCuentasConciliadas() {
        return cuentasConciliadas;
    }

    public void setCuentasConciliadas(Long cuentasConciliadas) {
        this.cuentasConciliadas = cuentasConciliadas;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
