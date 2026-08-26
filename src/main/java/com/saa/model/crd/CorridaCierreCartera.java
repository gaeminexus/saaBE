package com.saa.model.crd;

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
 * Representa la tabla CRCT (CorridaCierreCartera).
 *
 * Cabecera del proceso mensual de apertura/cierre de cartera: una fila por
 * (empresa, año, mes cerrado). Ver §3.2 de
 * docs/logica-negocio/crd/LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md.
 *
 * <b>Dos fechas y las dos importan.</b>
 * <ul>
 * <li>{@code fechaCorte} = ÚLTIMO día del mes cerrado. Es el filtro de "cuotas/intereses
 * pendientes con fecha &lt;= corte" (regla 1 de §5) y la fecha contable del NETEO.</li>
 * <li>{@code fechaProceso} = PRIMER día del mes siguiente. Es la fecha contable de
 * vencidos, bandas, apertura e intereses ("1er día del mes", §3.2), y la fecha con la que
 * se resuelve la configuración de bandas vigente en {@code CRD.CBPR}.</li>
 * </ul>
 *
 * <b>Trampa de columna de estado.</b> El ciclo de vida vive en {@code CRCTIDST}
 * ({@code idEstado}, ver {@link com.saa.rubros.EstadoCorridaCierreCartera}), NO en
 * {@code CRCTESTD}, que es el 1 activo / 0 inactivo de la fila. Es la misma separación que
 * CLAUDE.md documenta para {@code CRD.PRST}.
 *
 * <b>Idempotencia.</b> El índice único funcional {@code UK_CRCT_PERIODO} impide una segunda
 * corrida PREPARADA o EJECUTADA del mismo período; las REVERSADAS quedan fuera del índice,
 * que es lo que permite reprocesar un mes.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRCT", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CorridaCierreCarteraAll",
                query = "select e from CorridaCierreCartera e"),
    @NamedQuery(name = "CorridaCierreCarteraId",
                query = "select e from CorridaCierreCartera e where e.codigo = :id")
})
public class CorridaCierreCartera implements Serializable, EntidadAuditableFechaHora {

    /** Código de la corrida. PK autoincremental (IDENTITY, no secuencia). */
    @Id
    @Basic
    @Column(name = "CRCTCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Empresa (nodo SCP.PJRQ de nivel empresa). */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /** Año del mes CERRADO por la corrida, no el año en que se ejecuta. */
    @Basic
    @Column(name = "CRCTANOO")
    private Long anio;

    /** Mes CERRADO por la corrida, 1 a 12. */
    @Basic
    @Column(name = "CRCTMESS")
    private Long mes;

    /** Fecha de corte: último día del mes cerrado. */
    @Basic
    @Column(name = "CRCTFCCR")
    private LocalDate fechaCorte;

    /** Fecha de proceso: primer día del mes siguiente. */
    @Basic
    @Column(name = "CRCTFCPR")
    private LocalDate fechaProceso;

    /**
     * Estado OPERATIVO: 1 = PREPARADA, 2 = EJECUTADA, 3 = REVERSADA.
     * Ver {@link com.saa.rubros.EstadoCorridaCierreCartera}. NO es {@code estado}.
     */
    @Basic
    @Column(name = "CRCTIDST")
    private Long idEstado;

    /** Observación libre de la corrida. */
    @Basic
    @Column(name = "CRCTOBSR", length = 2000)
    private String observacion;

    /** Fecha y hora de registro. */
    @Basic
    @Column(name = "CRCTFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que registró. */
    @Basic
    @Column(name = "CRCTUSRG", length = 2000)
    private String usuarioRegistro;

    /** IP desde la que se registró. */
    @Basic
    @Column(name = "CRCTIPRG", length = 50)
    private String ipRegistro;

    /** Fecha y hora de la última modificación. */
    @Basic
    @Column(name = "CRCTFCMD")
    private LocalDateTime fechaModificacion;

    /** Usuario que modificó. */
    @Basic
    @Column(name = "CRCTUSMD", length = 2000)
    private String usuarioModificacion;

    /** IP desde la que se modificó. */
    @Basic
    @Column(name = "CRCTIPMD", length = 50)
    private String ipModificacion;

    /** Estado de la fila: 1 = activo, 0 = inactivo. Ver {@link com.saa.rubros.Estado}. */
    @Basic
    @Column(name = "CRCTESTD")
    private Long estado;

    public CorridaCierreCartera() {
    }

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

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

    public Long getAnio() {
        return anio;
    }

    public void setAnio(Long anio) {
        this.anio = anio;
    }

    public Long getMes() {
        return mes;
    }

    public void setMes(Long mes) {
        this.mes = mes;
    }

    public LocalDate getFechaCorte() {
        return fechaCorte;
    }

    public void setFechaCorte(LocalDate fechaCorte) {
        this.fechaCorte = fechaCorte;
    }

    public LocalDate getFechaProceso() {
        return fechaProceso;
    }

    public void setFechaProceso(LocalDate fechaProceso) {
        this.fechaProceso = fechaProceso;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    @Override
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    @Override
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getIpRegistro() {
        return ipRegistro;
    }

    public void setIpRegistro(String ipRegistro) {
        this.ipRegistro = ipRegistro;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public String getIpModificacion() {
        return ipModificacion;
    }

    public void setIpModificacion(String ipModificacion) {
        this.ipModificacion = ipModificacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
