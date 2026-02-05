package com.saa.model.crd;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

	/**
	 * Representa la tabla APAS (Aportes).
	 */
	@SuppressWarnings("serial")
	@Entity
	@Table(name = "APAS", schema = "CRD")
	@NamedQueries({
	    @NamedQuery(name = "AporteAsoprepAll", query = "select e from AporteAsoprep e"),
	    @NamedQuery(name = "AporteAsoprepId", query = "select e from AporteAsoprep e where e.cuenta = :id")
	})
	public class AporteAsoprep implements Serializable {

	    // ============================================================
	    // CLAVE PRIMARIA COMPUESTA
	    // ============================================================

	    @Id
	    @Basic
	    @Column(name = "APASCNTA")
	    private Long cuenta;

	    @Id
	    @Basic
	    @Column(name = "APASNSTT")
	    private Long institucion;

	    // ============================================================
	    // CAMPOS
	    // ============================================================

	    @Basic
	    @Column(name = "APASPRDT")
	    private Long producto;

	    @Basic
	    @Column(name = "APASNMRC")
	    private Long numeroCuenta;

	    @Basic
	    @Column(name = "APASFCHP")
	    private LocalDate fechaApertura;

	    @Basic
	    @Column(name = "APASSLDC", precision = 12, scale = 2)
	    private BigDecimal saldoCuenta;

	    @Basic
	    @Column(name = "APASSLDA", precision = 12, scale = 2)
	    private BigDecimal saldoAporte;

	    @Basic
	    @Column(name = "APASSLDI", precision = 12, scale = 2)
	    private BigDecimal saldoInteres;

	    @Basic
	    @Column(name = "APASFCHV")
	    private LocalDate fechaUltProvision;

	    @Basic
	    @Column(name = "APASOBSR", length = 60)
	    private String observaciones;

	    @Basic
	    @Column(name = "APASSTDO")
	    private Long estado;

	    @Basic
	    @Column(name = "APASCRDP")
	    private Long creadoPor;

	    @Basic
	    @Column(name = "APASFCHC")
	    private LocalDateTime fechaCreado;

	    @Basic
	    @Column(name = "APASACTP")
	    private Long actualizadoPor;

	    @Basic
	    @Column(name = "APASFCHA")
	    private LocalDateTime fechaActualiza;

	    @Basic
	    @Column(name = "APASCLNT")
	    private Long cliente;

	    @Basic
	    @Column(name = "APASFCHL")
	    private LocalDate fechaLiquida;

	    @Basic
	    @Column(name = "APASFCHM")
	    private LocalDate fechaUltMovimiento;

	    @Basic
	    @Column(name = "APASRGMN")
	    private Long regimen;

	    @Basic
	    @Column(name = "APASVLRA", precision = 12, scale = 2)
	    private BigDecimal valorUltAporte;

	    @Basic
	    @Column(name = "APASNMAP")
	    private Long numeroAporte;

	    @Basic
	    @Column(name = "APASFCHR")
	    private LocalDate fechaRenuncia;

	    @Basic
	    @Column(name = "APASNMSL", length = 30)
	    private String numeroSolicitud;

	    @Basic
	    @Column(name = "APASTPLQ")
	    private Long tipoLiquidacion;

	    @Basic
	    @Column(name = "APASACML")
	    private Long acumular;

	    // ============================================================
	    // GETTERS Y SETTERS
	    // ============================================================

	    public Long getCuenta() {
	        return cuenta;
	    }

	    public void setCuenta(Long cuenta) {
	        this.cuenta = cuenta;
	    }

	    public Long getInstitucion() {
	        return institucion;
	    }

	    public void setInstitucion(Long institucion) {
	        this.institucion = institucion;
	    }

	    public Long getProducto() {
	        return producto;
	    }

	    public void setProducto(Long producto) {
	        this.producto = producto;
	    }

	    public Long getNumeroCuenta() {
	        return numeroCuenta;
	    }

	    public void setNumeroCuenta(Long numeroCuenta) {
	        this.numeroCuenta = numeroCuenta;
	    }

	    public LocalDate getFechaApertura() {
	        return fechaApertura;
	    }

	    public void setFechaApertura(LocalDate fechaApertura) {
	        this.fechaApertura = fechaApertura;
	    }

	    public BigDecimal getSaldoCuenta() {
	        return saldoCuenta;
	    }

	    public void setSaldoCuenta(BigDecimal saldoCuenta) {
	        this.saldoCuenta = saldoCuenta;
	    }

	    public BigDecimal getSaldoAporte() {
	        return saldoAporte;
	    }

	    public void setSaldoAporte(BigDecimal saldoAporte) {
	        this.saldoAporte = saldoAporte;
	    }

	    public BigDecimal getSaldoInteres() {
	        return saldoInteres;
	    }

	    public void setSaldoInteres(BigDecimal saldoInteres) {
	        this.saldoInteres = saldoInteres;
	    }

	    public LocalDate getFechaUltProvision() {
	        return fechaUltProvision;
	    }

	    public void setFechaUltProvision(LocalDate fechaUltProvision) {
	        this.fechaUltProvision = fechaUltProvision;
	    }

	    public String getObservaciones() {
	        return observaciones;
	    }

	    public void setObservaciones(String observaciones) {
	        this.observaciones = observaciones;
	    }

	    public Long getEstado() {
	        return estado;
	    }

	    public void setEstado(Long estado) {
	        this.estado = estado;
	    }

	    public Long getCreadoPor() {
	        return creadoPor;
	    }

	    public void setCreadoPor(Long creadoPor) {
	        this.creadoPor = creadoPor;
	    }

	    public LocalDateTime getFechaCreado() {
	        return fechaCreado;
	    }

	    public void setFechaCreado(LocalDateTime fechaCreado) {
	        this.fechaCreado = fechaCreado;
	    }

	    public Long getActualizadoPor() {
	        return actualizadoPor;
	    }

	    public void setActualizadoPor(Long actualizadoPor) {
	        this.actualizadoPor = actualizadoPor;
	    }

	    public LocalDateTime getFechaActualiza() {
	        return fechaActualiza;
	    }

	    public void setFechaActualiza(LocalDateTime fechaActualiza) {
	        this.fechaActualiza = fechaActualiza;
	    }

	    public Long getCliente() {
	        return cliente;
	    }

	    public void setCliente(Long cliente) {
	        this.cliente = cliente;
	    }

	    public LocalDate getFechaLiquida() {
	        return fechaLiquida;
	    }

	    public void setFechaLiquida(LocalDate fechaLiquida) {
	        this.fechaLiquida = fechaLiquida;
	    }

	    public LocalDate getFechaUltMovimiento() {
	        return fechaUltMovimiento;
	    }

	    public void setFechaUltMovimiento(LocalDate fechaUltMovimiento) {
	        this.fechaUltMovimiento = fechaUltMovimiento;
	    }

	    public Long getRegimen() {
	        return regimen;
	    }

	    public void setRegimen(Long regimen) {
	        this.regimen = regimen;
	    }

	    public BigDecimal getValorUltAporte() {
	        return valorUltAporte;
	    }

	    public void setValorUltAporte(BigDecimal valorUltAporte) {
	        this.valorUltAporte = valorUltAporte;
	    }

	    public Long getNumeroAporte() {
	        return numeroAporte;
	    }

	    public void setNumeroAporte(Long numeroAporte) {
	        this.numeroAporte = numeroAporte;
	    }

	    public LocalDate getFechaRenuncia() {
	        return fechaRenuncia;
	    }

	    public void setFechaRenuncia(LocalDate fechaRenuncia) {
	        this.fechaRenuncia = fechaRenuncia;
	    }

	    public String getNumeroSolicitud() {
	        return numeroSolicitud;
	    }

	    public void setNumeroSolicitud(String numeroSolicitud) {
	        this.numeroSolicitud = numeroSolicitud;
	    }

	    public Long getTipoLiquidacion() {
	        return tipoLiquidacion;
	    }

	    public void setTipoLiquidacion(Long tipoLiquidacion) {
	        this.tipoLiquidacion = tipoLiquidacion;
	    }

	    public Long getAcumular() {
	        return acumular;
	    }

	    public void setAcumular(Long acumular) {
	        this.acumular = acumular;
	    }

	    
}
