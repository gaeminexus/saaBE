package com.saa.model.crd;

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
 * Representa la tabla ANCC (AsientoCierreCartera).
 *
 * Un registro por sub-proceso contabilizado en una corrida: qué asiento salió de qué
 * sub-proceso, por cuánto y con cuántas líneas. Da trazabilidad y es lo que el reverso
 * recorre para anular.
 *
 * <b>{@code asiento} es un número sin FK, a propósito.</b> El sistema se comercializa
 * después SIN el módulo crd, así que no puede quedar integridad referencial entre CRD y CNT
 * en la dirección de los documentos. Mismo criterio que {@code CRD.PGPR.PGPRASNT} y
 * {@code CRD.DVAP}. La consistencia la garantiza este proceso, no la base.
 *
 * <b>Trampa de columna de estado.</b> El ciclo de vida del asiento vive en {@code ANCCIDST}
 * ({@code idEstado}: 1 GENERADO, 2 ANULADO — los valores de
 * {@link com.saa.rubros.EstadoAsiento}), no en {@code ANCCESTD}.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ANCC", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "AsientoCierreCarteraAll",
                query = "select e from AsientoCierreCartera e"),
    @NamedQuery(name = "AsientoCierreCarteraId",
                query = "select e from AsientoCierreCartera e where e.codigo = :id")
})
public class AsientoCierreCartera implements Serializable, EntidadAuditableFechaHora {

    /** Código de la fila. PK autoincremental. */
    @Id
    @Basic
    @Column(name = "ANCCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Corrida a la que pertenece el asiento. */
    @ManyToOne
    @JoinColumn(name = "CRCTCDGO", referencedColumnName = "CRCTCDGO")
    private CorridaCierreCartera corrida;

    /**
     * Sub-proceso que generó el asiento: 1 vencidos, 2 cambio de bandas por vencer,
     * 3 cambio de bandas vencido, 4 apertura, 5 devengo de intereses, 6 neteo.
     * Ver {@link com.saa.rubros.SubProcesoCierreCartera}.
     */
    @Basic
    @Column(name = "ANCCTPOO")
    private Long subProceso;

    /** Id del asiento generado en {@code CNT.ASNT}. Número sin FK, ver el javadoc de la clase. */
    @Basic
    @Column(name = "ANCCASNT")
    private Long asiento;

    /** Número del asiento, para mostrarlo sin ir a CNT. */
    @Basic
    @Column(name = "ANCCNMAS", length = 50)
    private String numeroAsiento;

    /** Fecha contable del asiento. */
    @Basic
    @Column(name = "ANCCFCHA")
    private LocalDate fecha;

    /** Total del asiento: el DEBE, que iguala al HABER. */
    @Basic
    @Column(name = "ANCCVLRR")
    private Double valor;

    /** Cantidad de líneas del asiento. */
    @Basic
    @Column(name = "ANCCCNTD")
    private Long cantidad;

    /** Estado OPERATIVO: 1 = GENERADO, 2 = ANULADO por reverso. NO es {@code estado}. */
    @Basic
    @Column(name = "ANCCIDST")
    private Long idEstado;

    /** Fecha y hora de registro. */
    @Basic
    @Column(name = "ANCCFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que registró. */
    @Basic
    @Column(name = "ANCCUSRG", length = 2000)
    private String usuarioRegistro;

    /** IP desde la que se registró. */
    @Basic
    @Column(name = "ANCCIPRG", length = 50)
    private String ipRegistro;

    /** Fecha y hora de la última modificación. */
    @Basic
    @Column(name = "ANCCFCMD")
    private LocalDateTime fechaModificacion;

    /** Usuario que modificó. */
    @Basic
    @Column(name = "ANCCUSMD", length = 2000)
    private String usuarioModificacion;

    /** IP desde la que se modificó. */
    @Basic
    @Column(name = "ANCCIPMD", length = 50)
    private String ipModificacion;

    /** Estado de la fila: 1 = activo, 0 = inactivo. */
    @Basic
    @Column(name = "ANCCESTD")
    private Long estado;

    public AsientoCierreCartera() {
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

    public CorridaCierreCartera getCorrida() {
        return corrida;
    }

    public void setCorrida(CorridaCierreCartera corrida) {
        this.corrida = corrida;
    }

    public Long getSubProceso() {
        return subProceso;
    }

    public void setSubProceso(Long subProceso) {
        this.subProceso = subProceso;
    }

    public Long getAsiento() {
        return asiento;
    }

    public void setAsiento(Long asiento) {
        this.asiento = asiento;
    }

    public String getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(String numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
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
