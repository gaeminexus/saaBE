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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.ANCP (AsientoCargaPetro).
 *
 * Espejo DELIBERADO de {@link AsientoCierreCartera} (CRD.ANCC), que ya está en producción
 * para el cierre de cartera: un registro por sub-proceso contable de una carga Petro, con
 * qué asiento generó, por cuánto y con cuántas líneas — trazabilidad y lo que recorre el
 * reverso.
 *
 * <b>{@code asiento} es un número sin FK en el modelo de negocio</b> pero la tabla SÍ tiene
 * FK física a {@code CNT.ASNT} (a diferencia de ANCC): se agregó en la revisión del DDL,
 * mismo criterio que la corrección que recibió ANCC.
 *
 * <b>Un solo estado, más simple que ANCC.</b> {@code idEstado} (`ANCPIDST`) es 1 = VIGENTE,
 * 0 = REVERSADO — no hay columna de estado de fila separada, a diferencia de ANCC que
 * arrastra `ANCCESTD` genérico además de `ANCCIDST`.
 *
 * @see com.saa.rubros.SubProcesoCobroPetro
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ANCP", schema = "CRD")
@SequenceGenerator(name = "SQ_ANCPCDGO", sequenceName = "CRD.SQ_ANCPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AsientoCargaPetroAll",
                query = "select e from AsientoCargaPetro e"),
    @NamedQuery(name = "AsientoCargaPetroId",
                query = "select e from AsientoCargaPetro e where e.codigo = :id")
})
public class AsientoCargaPetro implements Serializable, EntidadAuditableFechaHora {

    /** Código de la fila. PK. */
    @Id
    @Basic
    @Column(name = "ANCPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ANCPCDGO")
    private Long codigo;

    /** FK - Carga Petro a la que pertenece el asiento. */
    @ManyToOne
    @JoinColumn(name = "CRARCDGO", referencedColumnName = "CRARCDGO")
    private CargaArchivo cargaArchivo;

    /**
     * Sub-proceso que generó el asiento: 1 TRANSITORIO, 2 REPARTO, 3 APLICACION.
     * Ver {@link com.saa.rubros.SubProcesoCobroPetro}.
     */
    @Basic
    @Column(name = "ANCPTPOO")
    private Long subProceso;

    /** Id del asiento generado en CNT.ASNT. */
    @Basic
    @Column(name = "ANCPASNT")
    private Long asiento;

    /** Número del asiento, denormalizado para consulta sin ir a CNT. */
    @Basic
    @Column(name = "ANCPNMAS", length = 50)
    private String numeroAsiento;

    /** Fecha contable del asiento. */
    @Basic
    @Column(name = "ANCPFCHA")
    private LocalDate fecha;

    /** Total del asiento: el Debe, que iguala al Haber. */
    @Basic
    @Column(name = "ANCPVLRR")
    private Double valor;

    /** Cantidad de líneas del asiento. */
    @Basic
    @Column(name = "ANCPCNTD")
    private Long cantidad;

    /** Observación libre. */
    @Basic
    @Column(name = "ANCPOBSR", length = 2000)
    private String observacion;

    /** Estado OPERATIVO: 1 = vigente, 0 = reversado. Único estado de esta tabla. */
    @Basic
    @Column(name = "ANCPIDST")
    private Long idEstado;

    /** Usuario que registró. */
    @Basic
    @Column(name = "ANCPUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha y hora de registro. */
    @Basic
    @Column(name = "ANCPFCRG")
    private LocalDateTime fechaRegistro;

    /** IP desde la que se registró. */
    @Basic
    @Column(name = "ANCPIPRG", length = 50)
    private String ipRegistro;

    public AsientoCargaPetro() {
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public CargaArchivo getCargaArchivo() {
        return cargaArchivo;
    }

    public void setCargaArchivo(CargaArchivo cargaArchivo) {
        this.cargaArchivo = cargaArchivo;
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

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    @Override
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    @Override
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getIpRegistro() {
        return ipRegistro;
    }

    public void setIpRegistro(String ipRegistro) {
        this.ipRegistro = ipRegistro;
    }
}
