package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

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
 * Representa la tabla EVPR (EventoPrestamo).
 *
 * Cabecera de TODA operación de pago sobre un préstamo: cada llamada de negocio
 * (pago manual, pago con aportes, abono a capital, precancelación) crea UN evento y
 * todos los PagoPrestamo, movimientos de aportes y cuotas historizadas de esa llamada
 * cuelgan de él. Es el punto de agrupación para el reverso y para el asiento contable.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "EVPR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "EventoPrestamoAll", query = "select e from EventoPrestamo e"),
    @NamedQuery(name = "EventoPrestamoId", query = "select e from EventoPrestamo e where e.codigo = :id")
})
public class EventoPrestamo implements Serializable {

    /** Código del evento */
    @Id
    @Basic
    @Column(name = "EVPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Código Prestamo */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** Tipo de operación: PAGO_MANUAL | PAGO_APORTES | ABONO_CAPITAL | PRECANCELACION */
    @Basic
    @Column(name = "EVPRTPOO", length = 30)
    private String tipoOperacion;

    /** Valor total de la operación */
    @Basic
    @Column(name = "EVPRVLRR")
    private Double valor;

    /** Modalidad del abono a capital: 1 = reduce plazo, 2 = reduce cuota. NULL en otros tipos */
    @Basic
    @Column(name = "EVPRMDLD")
    private Long modalidad;

    /** Plazo anterior (abono a capital) */
    @Basic
    @Column(name = "EVPRPLZA")
    private Long plazoAnterior;

    /** Plazo nuevo (abono a capital) */
    @Basic
    @Column(name = "EVPRPLZN")
    private Long plazoNuevo;

    /** Valor de cuota anterior (abono a capital) */
    @Basic
    @Column(name = "EVPRCTAA")
    private Double cuotaAnterior;

    /** Valor de cuota nuevo (abono a capital) */
    @Basic
    @Column(name = "EVPRCTNN")
    private Double cuotaNueva;

    /** Fecha de negocio de la operación */
    @Basic
    @Column(name = "EVPRFCHA")
    private LocalDateTime fecha;

    /**
     * Guarda {@code CNT.ASNT.ASNTCDGO} — la PK del asiento (lo que devuelven los hooks de
     * {@code ContabilidadPrestamoService}, javadoc "Código del asiento creado"), NO
     * {@code ASNTNMRO} (el correlativo por empresa/período). El nombre de la columna
     * ({@code EVPRNMAS}, "número de asiento") es engañoso pero es histórico: no se migra
     * (decisión del árbitro, 2026-08-31) — reversar por la PK es directo
     * ({@code AsientoService.anulaAsiento(Long idAsiento)} recibe el id, no el número);
     * reversar por {@code ASNTNMRO} exigiría resolver empresa + período, que es circular.
     */
    @Basic
    @Column(name = "EVPRNMAS")
    private Long numeroAsiento;

    /** Observación del usuario */
    @Basic
    @Column(name = "EVPROBSR", length = 2000)
    private String observacion;

    /** Usuario que ejecutó la operación */
    @Basic
    @Column(name = "EVPRUSAR", length = 50)
    private String usuario;

    /** Fecha de registro */
    @Basic
    @Column(name = "EVPRFCRG")
    private LocalDateTime fechaRegistro;

    /** Estado: 1 = vigente, 0 = anulado */
    @Basic
    @Column(name = "EVPRESTD")
    private Long estado;

    /** Usuario de anulación */
    @Basic
    @Column(name = "EVPRUSAN", length = 50)
    private String usuarioAnulacion;

    /** Fecha de anulación */
    @Basic
    @Column(name = "EVPRFCAN")
    private LocalDateTime fechaAnulacion;

    /** Motivo de la anulación */
    @Basic
    @Column(name = "EVPRMTAN", length = 500)
    private String motivoAnulacion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(String tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getModalidad() {
        return modalidad;
    }

    public void setModalidad(Long modalidad) {
        this.modalidad = modalidad;
    }

    public Long getPlazoAnterior() {
        return plazoAnterior;
    }

    public void setPlazoAnterior(Long plazoAnterior) {
        this.plazoAnterior = plazoAnterior;
    }

    public Long getPlazoNuevo() {
        return plazoNuevo;
    }

    public void setPlazoNuevo(Long plazoNuevo) {
        this.plazoNuevo = plazoNuevo;
    }

    public Double getCuotaAnterior() {
        return cuotaAnterior;
    }

    public void setCuotaAnterior(Double cuotaAnterior) {
        this.cuotaAnterior = cuotaAnterior;
    }

    public Double getCuotaNueva() {
        return cuotaNueva;
    }

    public void setCuotaNueva(Double cuotaNueva) {
        this.cuotaNueva = cuotaNueva;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Long getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(Long numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(String usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }
}
