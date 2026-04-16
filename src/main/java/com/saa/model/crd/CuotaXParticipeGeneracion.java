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
 * Representa la tabla CXPG (CuotaXParticipeGeneracion).
 * 
 * Almacena el desglose de qué cuotas de préstamo se están cancelando
 * con el monto enviado en la tabla PDGA.
 * 
 * PROPÓSITO:
 * Esta tabla proporciona trazabilidad completa de qué cuotas específicas
 * se incluyeron en cada línea del archivo de descuentos Petrocomercial.
 * 
 * Permite:
 * - Saber exactamente qué cuotas se sumaron en el monto enviado
 * - Auditar el proceso de generación del archivo
 * - Reconstruir el cálculo de montos
 * - Identificar cuotas anteriores pendientes que se incluyeron
 * 
 * RELACIONES:
 * - Pertenece a un ParticipeDetalleGeneracionArchivo (PDGA)
 * - Se vincula a un Prestamo (PRST)
 * 
 * EJEMPLO:
 * Si el monto enviado para un préstamo fue $400 que incluye:
 * - Cuota #1 (enero)   - $100
 * - Cuota #2 (febrero) - $100
 * - Cuota #4 (abril)   - $100
 * - Cuota #5 (mayo)    - $100  ← Cuota del mes
 * 
 * Se crearán 4 registros en CXPG, uno por cada cuota.
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CXPG", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CuotaXParticipeGeneracionAll", query = "select e from CuotaXParticipeGeneracion e"),
    @NamedQuery(name = "CuotaXParticipeGeneracionId", query = "select e from CuotaXParticipeGeneracion e where e.codigo = :id"),
    @NamedQuery(name = "CuotaXParticipeGeneracionByParticipe", query = "select e from CuotaXParticipeGeneracion e where e.participeDetalleGeneracion.codigo = :codigoParticipe order by e.numeroCuota")
})
public class CuotaXParticipeGeneracion implements Serializable {

    // ============================================================
    // PK
    // ============================================================
    
    /**
     * Código único del registro (IDENTITY).
     */
    @Id
    @Basic
    @Column(name = "CXPGCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    // ============================================================
    // RELACIONES (FKs)
    // ============================================================
    
    /**
     * FK - Partícipe Detalle Generación (línea del archivo).
     */
    @ManyToOne
    @JoinColumn(name = "PDGACDGO", referencedColumnName = "PDGACDGO")
    private ParticipeDetalleGeneracionArchivo participeDetalleGeneracion;
    
    /**
     * FK - Préstamo asociado.
     */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;
    
    /**
     * FK - Tipo de Aporte (solo se llena para aportes, producto AH).
     * Valores: 9 = Jubilación, 11 = Cesantía
     */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    // ============================================================
    // DATOS DE LA CUOTA
    // ============================================================
    
    /**
     * Número de cuota que se está cancelando.
     */
    @Basic
    @Column(name = "CXPGNNCT")
    private Integer numeroCuota;
    
    /**
     * Valor de la cuota aplicado.
     * Este es el monto que aporta esta cuota al total enviado.
     */
    @Basic
    @Column(name = "CXPGVLCT")
    private Double valorCuota;

    // ============================================================
    // AUDITORÍA
    // ============================================================
    
    /**
     * Usuario ingreso.
     */
    @Basic
    @Column(name = "CXPGUSIN", length = 50)
    private String usuarioIngreso;
    
    /**
     * Fecha ingreso.
     */
    @Basic
    @Column(name = "CXPGFCIN")
    private LocalDateTime fechaIngreso;
    
    /**
     * Usuario modificación.
     */
    @Basic
    @Column(name = "CXPGUSMD", length = 50)
    private String usuarioModificacion;
    
    /**
     * Fecha modificación.
     */
    @Basic
    @Column(name = "CXPGFCMD")
    private LocalDateTime fechaModificacion;

    // ============================================================
    // CONSTRUCTORES
    // ============================================================
    
    public CuotaXParticipeGeneracion() {
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

    public ParticipeDetalleGeneracionArchivo getParticipeDetalleGeneracion() {
        return participeDetalleGeneracion;
    }

    public void setParticipeDetalleGeneracion(ParticipeDetalleGeneracionArchivo participeDetalleGeneracion) {
        this.participeDetalleGeneracion = participeDetalleGeneracion;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public TipoAporte getTipoAporte() {
        return tipoAporte;
    }

    public void setTipoAporte(TipoAporte tipoAporte) {
        this.tipoAporte = tipoAporte;
    }

    public Integer getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Integer numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public Double getValorCuota() {
        return valorCuota;
    }

    public void setValorCuota(Double valorCuota) {
        this.valorCuota = valorCuota;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
