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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.DEXB.
 * Entity DetalleExtractoBancario.
 * Una fila por transaccion bancaria importada, normalizada a un formato
 * unico sin importar el banco de origen. Fuente de verdad para verificar
 * contra los movimientos del libro (MovimientoBanco).</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DEXB", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "DetalleExtractoBancarioAll",
        query = "select e from DetalleExtractoBancario e order by e.cuentaBancaria.codigo, e.fechaTransaccion"),
    @NamedQuery(name = "DetalleExtractoBancarioId",
        query = "select e from DetalleExtractoBancario e where e.codigo = :id")
})
public class DetalleExtractoBancario implements Serializable {

    /**
     * Código / PK autoincrementable.
     */
    @Basic
    @Id
    @Column(name = "DEXBCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK al Extracto Bancario (archivo) al que pertenece esta fila.
     */
    @ManyToOne
    @JoinColumn(name = "EXBCCDGO", nullable = false)
    private ExtractoBancario extractoBancario;

    /**
     * FK a la Cuenta Bancaria (denormalizado para consulta directa sin pasar por ExtractoBancario).
     */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", nullable = false)
    private CuentaBancaria cuentaBancaria;

    /**
     * FK al Periodo Contable (CNT.PRDO). Denormalizado desde el ExtractoBancario
     * padre para poder filtrar el detalle por periodo sin pasar por el join -
     * mismo patron que DetalleConciliacion.periodo.
     */
    @ManyToOne
    @JoinColumn(name = "PRDOCDGO", nullable = false)
    private Periodo periodo;

    /**
     * Fecha de la transaccion.
     */
    @Basic
    @Column(name = "DEXBFTRN")
    private LocalDate fechaTransaccion;

    /**
     * Fecha contable/valuta, si el banco la distingue de la fecha de transaccion (nullable).
     */
    @Basic
    @Column(name = "DEXBFCNT")
    private LocalDate fechaContable;

    /**
     * Descripcion / concepto de la transaccion, normalizado.
     */
    @Basic
    @Column(name = "DEXBDSCR", length = 500)
    private String descripcion;

    /**
     * Referencia o numero de documento del banco.
     */
    @Basic
    @Column(name = "DEXBREFR", length = 100)
    private String referencia;

    /**
     * Codigo de movimiento tal como lo reporta el banco (TW, DP, N/C, CABE, ...).
     * Se conserva textual para trazabilidad, no se usa para filtrar debito/credito.
     */
    @Basic
    @Column(name = "DEXBCDMV", length = 20)
    private String codigoMovimiento;

    /**
     * Valor debito.
     */
    @Basic
    @Column(name = "DEXBDBTO")
    private Double debito;

    /**
     * Valor credito.
     */
    @Basic
    @Column(name = "DEXBCRDT")
    private Double credito;

    /**
     * Saldo reportado por el banco tras esta transaccion (nullable).
     */
    @Basic
    @Column(name = "DEXBSLDO")
    private Double saldo;

    /**
     * Hash de deduplicacion: sha256(cuenta, fecha, debito, credito, descripcion, saldo).
     */
    @Basic
    @Column(name = "DEXBHASH", length = 64)
    private String hash;

    /**
     * Numero de fila en el archivo origen (trazabilidad).
     */
    @Basic
    @Column(name = "DEXBNFIL")
    private Long numeroFila;

    /**
     * Fila cruda tal como fue extraida del archivo, antes de normalizar (auditoria).
     */
    @Lob
    @Basic
    @Column(name = "DEXBCRDO")
    private String filaCruda;

    /**
     * FK al Movimiento de Banco (libro) con el que fue conciliada esta fila.
     * Nullable - el motor de conciliacion es trabajo futuro.
     */
    @ManyToOne
    @JoinColumn(name = "DEXBCNCL")
    private MovimientoBanco movimientoConciliado;

    /**
     * Estado de revision de esta fila.
     * Referencia al rubro con código alterno 173 (ASPEstadoRevisionExtracto):
     * 1=Pendiente de Revision, 2=Conciliada, 3=Descartada.
     */
    @Basic
    @Column(name = "DEXBESTR")
    private Long estadoRevision;

    /**
     * Fecha y hora de creación del registro (auditoría).
     */
    @Basic
    @Column(name = "DEXBFCRG")
    private LocalDateTime fechaCreacion;

    /**
     * Usuario que cargó el registro (auditoría).
     */
    @Basic
    @Column(name = "DEXBUSAR", length = 50)
    private String usuarioCreacion;

    /**
     * Estado del registro. 1 = Activo, 0 = Inactivo.
     */
    @Basic
    @Column(name = "DEXBESTD")
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

    public ExtractoBancario getExtractoBancario() {
        return extractoBancario;
    }

    public void setExtractoBancario(ExtractoBancario extractoBancario) {
        this.extractoBancario = extractoBancario;
    }

    public CuentaBancaria getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Periodo periodo) {
        this.periodo = periodo;
    }

    public LocalDate getFechaTransaccion() {
        return fechaTransaccion;
    }

    public void setFechaTransaccion(LocalDate fechaTransaccion) {
        this.fechaTransaccion = fechaTransaccion;
    }

    public LocalDate getFechaContable() {
        return fechaContable;
    }

    public void setFechaContable(LocalDate fechaContable) {
        this.fechaContable = fechaContable;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getCodigoMovimiento() {
        return codigoMovimiento;
    }

    public void setCodigoMovimiento(String codigoMovimiento) {
        this.codigoMovimiento = codigoMovimiento;
    }

    public Double getDebito() {
        return debito;
    }

    public void setDebito(Double debito) {
        // DEXBDBTO es NOT NULL en Oracle - los parsers de bancos con una sola
        // columna de monto (mas signo/tipo de movimiento) dejan este valor en
        // null para las filas de credito.
        this.debito = debito != null ? debito : 0.0;
    }

    public Double getCredito() {
        return credito;
    }

    public void setCredito(Double credito) {
        // DEXBCRDT es NOT NULL en Oracle - ver comentario en setDebito().
        this.credito = credito != null ? credito : 0.0;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Long getNumeroFila() {
        return numeroFila;
    }

    public void setNumeroFila(Long numeroFila) {
        this.numeroFila = numeroFila;
    }

    public String getFilaCruda() {
        return filaCruda;
    }

    public void setFilaCruda(String filaCruda) {
        this.filaCruda = filaCruda;
    }

    public MovimientoBanco getMovimientoConciliado() {
        return movimientoConciliado;
    }

    public void setMovimientoConciliado(MovimientoBanco movimientoConciliado) {
        this.movimientoConciliado = movimientoConciliado;
    }

    /**
     * @return estadoRevision (rubro 173: 1=Pendiente de Revision, 2=Conciliada, 3=Descartada)
     */
    public Long getEstadoRevision() {
        return estadoRevision;
    }

    public void setEstadoRevision(Long estadoRevision) {
        this.estadoRevision = estadoRevision;
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
