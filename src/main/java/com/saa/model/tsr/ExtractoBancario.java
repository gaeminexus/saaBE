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
 * <p>Pojo mapeo de tabla TSR.EXBC.
 * Entity ExtractoBancario.
 * Encabezado de cada estado de cuenta bancario importado desde Excel/PDF -
 * un registro por archivo cargado. El detalle transaccional vive en
 * DetalleExtractoBancario.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "EXBC", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "ExtractoBancarioAll",
        query = "select e from ExtractoBancario e "
            + "join fetch e.cuentaBancaria "
            + "join fetch e.cuentaBancaria.banco "
            + "join fetch e.empresa "
            + "order by e.fechaCreacion desc"),
    @NamedQuery(name = "ExtractoBancarioId",
        query = "select e from ExtractoBancario e where e.codigo = :id"),
    @NamedQuery(name = "ExtractoBancarioByHash",
        query = "select e from ExtractoBancario e where e.archivoHash = :hash")
})
public class ExtractoBancario implements Serializable {

    /**
     * Código / PK autoincrementable.
     */
    @Basic
    @Id
    @Column(name = "EXBCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK a la Cuenta Bancaria a la que pertenece este extracto.
     */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", nullable = false)
    private CuentaBancaria cuentaBancaria;

    /**
     * FK a la Empresa.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", nullable = false)
    private Empresa empresa;

    /**
     * Nombre del archivo origen.
     */
    @Basic
    @Column(name = "EXBCARCH", length = 255)
    private String archivoNombre;

    /**
     * SHA-256 del archivo origen. Evita recargar el mismo archivo dos veces.
     */
    @Basic
    @Column(name = "EXBCHASH", length = 64)
    private String archivoHash;

    /**
     * Formato del archivo origen: XLS, XLSX, PDF o CSV.
     */
    @Basic
    @Column(name = "EXBCFRMT", length = 10)
    private String formato;

    /**
     * Identificador y version del parser/adaptador usado para interpretar el archivo.
     */
    @Basic
    @Column(name = "EXBCPRSR", length = 50)
    private String parser;

    /**
     * Periodo cubierto por el extracto: desde.
     */
    @Basic
    @Column(name = "EXBCFDSD")
    private LocalDate fechaDesde;

    /**
     * Periodo cubierto por el extracto: hasta.
     */
    @Basic
    @Column(name = "EXBCFHST")
    private LocalDate fechaHasta;

    /**
     * Saldo inicial segun el banco.
     */
    @Basic
    @Column(name = "EXBCSLIN")
    private Double saldoInicial;

    /**
     * Saldo final segun el banco.
     */
    @Basic
    @Column(name = "EXBCSLFN")
    private Double saldoFinal;

    /**
     * Estado de carga del archivo.
     * Referencia al rubro con código alterno 172 (ASPEstadoCargaExtracto):
     * 0=Raiz, 1=Cargado, 2=Validado, 3=Aplicado, 4=Error.
     */
    @Basic
    @Column(name = "EXBCESTP")
    private Long estadoCarga;

    /**
     * Detalle de error si estadoCarga=Error, o notas de revision.
     */
    @Basic
    @Column(name = "EXBCOBSR", length = 1000)
    private String observaciones;

    /**
     * Fecha y hora de creación del registro (auditoría).
     */
    @Basic
    @Column(name = "EXBCFCRG")
    private LocalDateTime fechaCreacion;

    /**
     * Usuario que cargó el archivo (auditoría).
     */
    @Basic
    @Column(name = "EXBCUSAR", length = 50)
    private String usuarioCreacion;

    /**
     * Estado del registro. 1 = Activo, 0 = Inactivo.
     */
    @Basic
    @Column(name = "EXBCESTD")
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

    public CuentaBancaria getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getArchivoNombre() {
        return archivoNombre;
    }

    public void setArchivoNombre(String archivoNombre) {
        this.archivoNombre = archivoNombre;
    }

    public String getArchivoHash() {
        return archivoHash;
    }

    public void setArchivoHash(String archivoHash) {
        this.archivoHash = archivoHash;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getParser() {
        return parser;
    }

    public void setParser(String parser) {
        this.parser = parser;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Double getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(Double saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public Double getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(Double saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    /**
     * @return estadoCarga (rubro 172: 0=Raiz, 1=Cargado, 2=Validado, 3=Aplicado, 4=Error)
     */
    public Long getEstadoCarga() {
        return estadoCarga;
    }

    public void setEstadoCarga(Long estadoCarga) {
        this.estadoCarga = estadoCarga;
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

    public String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    public void setUsuarioCreacion(String usuarioCreacion) {
        this.usuarioCreacion = usuarioCreacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
