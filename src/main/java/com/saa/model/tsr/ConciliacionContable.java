/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.cnt.Periodo;

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
 * <p>Pojo mapeo de tabla TSR.CNCT.
 * Entity ConciliacionContable.
 * Cabecera de la conciliación de extracto bancario (DEXB) contra
 * contabilidad (DetalleAsiento) - una fila por cuenta bancaria + período.
 * Es un proceso distinto e independiente de la conciliación de
 * cheques/depósitos que ya existe (Conciliacion/DetalleConciliacion): esta
 * entidad conecta el extracto bancario importado con los asientos contables
 * ya existentes, vía {@link GrupoConciliacionContable}.</p>
 * <p>Los contadores (totalGrupos, totalPendientesExtracto,
 * totalPendientesAsiento) son un resumen recalculado tras cada
 * conciliación/deshecho de grupo - no la fuente de verdad, que siempre es
 * contar los registros reales de DEXB/DetalleAsiento sin grupo asignado.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNCT", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "ConciliacionContableAll", query = "select e from ConciliacionContable e"),
    @NamedQuery(name = "ConciliacionContableId", query = "select e from ConciliacionContable e where e.codigo = :id")
})
public class ConciliacionContable implements Serializable {

    /**
     * Código / PK autoincrementable.
     */
    @Basic
    @Id
    @Column(name = "CNCTCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK a la Cuenta Bancaria que se concilia.
     */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", nullable = false)
    private CuentaBancaria cuentaBancaria;

    /**
     * FK al Periodo Contable de esta conciliación.
     */
    @ManyToOne
    @JoinColumn(name = "PRDOCDGO", nullable = false)
    private Periodo periodo;

    /**
     * Estado de revisión (rubro EstadoConciliacionContable: 1=Pendiente,
     * 2=Verificado, 3=Con diferencias).
     */
    @Basic
    @Column(name = "CNCTESTR")
    private Long estadoRevision;

    /**
     * Cantidad de grupos de conciliación (GrupoConciliacionContable) vigentes
     * para esta cuenta/período. Recalculado, no autoritativo.
     */
    @Basic
    @Column(name = "CNCTTTGR")
    private Long totalGrupos;

    /**
     * Cantidad de filas de DetalleExtractoBancario sin conciliar todavía.
     * Recalculado, no autoritativo.
     */
    @Basic
    @Column(name = "CNCTPDEX")
    private Long totalPendientesExtracto;

    /**
     * Cantidad de filas de DetalleAsiento (dentro del alcance de esta cuenta)
     * sin conciliar todavía. Recalculado, no autoritativo.
     */
    @Basic
    @Column(name = "CNCTPDAS")
    private Long totalPendientesAsiento;

    /**
     * Usuario que marcó esta cuenta/período como verificado.
     */
    @Basic
    @Column(name = "CNCTUSVR", length = 50)
    private String usuarioVerifica;

    /**
     * Fecha en que se marcó como verificado.
     */
    @Basic
    @Column(name = "CNCTFCVR")
    private LocalDateTime fechaVerificacion;

    /**
     * Fecha y hora de creación del registro (auditoría).
     */
    @Basic
    @Column(name = "CNCTFCRG")
    private LocalDateTime fechaCreacion;

    /**
     * Estado del registro. 1 = Activo, 0 = Inactivo.
     */
    @Basic
    @Column(name = "CNCTESTD")
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

    public Periodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Periodo periodo) {
        this.periodo = periodo;
    }

    /**
     * @return estadoRevision (rubro EstadoConciliacionContable)
     */
    public Long getEstadoRevision() {
        return estadoRevision;
    }

    public void setEstadoRevision(Long estadoRevision) {
        this.estadoRevision = estadoRevision;
    }

    public Long getTotalGrupos() {
        return totalGrupos;
    }

    public void setTotalGrupos(Long totalGrupos) {
        this.totalGrupos = totalGrupos;
    }

    public Long getTotalPendientesExtracto() {
        return totalPendientesExtracto;
    }

    public void setTotalPendientesExtracto(Long totalPendientesExtracto) {
        this.totalPendientesExtracto = totalPendientesExtracto;
    }

    public Long getTotalPendientesAsiento() {
        return totalPendientesAsiento;
    }

    public void setTotalPendientesAsiento(Long totalPendientesAsiento) {
        this.totalPendientesAsiento = totalPendientesAsiento;
    }

    public String getUsuarioVerifica() {
        return usuarioVerifica;
    }

    public void setUsuarioVerifica(String usuarioVerifica) {
        this.usuarioVerifica = usuarioVerifica;
    }

    public LocalDateTime getFechaVerificacion() {
        return fechaVerificacion;
    }

    public void setFechaVerificacion(LocalDateTime fechaVerificacion) {
        this.fechaVerificacion = fechaVerificacion;
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
