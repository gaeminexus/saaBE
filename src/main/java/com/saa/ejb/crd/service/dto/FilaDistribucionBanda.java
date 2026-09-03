package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Un renglón del detalle de distribución en bandas — API-AUDITORIA-BANDAS.md §2.
 * {@code cuentaContable}/{@code nombreCuenta}/{@code idAsiento} vienen null con contabilidad
 * desconectada; no es un error. Los campos de banda vienen null salvo en concepto
 * {@code CAPITAL} — ausencia de dato legítima.
 */
public class FilaDistribucionBanda {

    private Long id;
    private String concepto;
    private Double valor;

    private Long idEntidad;
    private String participe;
    private String cedula;
    private Long codigoAsoprep;

    private Long idPrestamo;
    private Long numeroCuota;
    private LocalDate fechaVencimiento;
    private LocalDate fechaAplicacion;

    private Long idProducto;
    private String producto;
    private Long idTipoPrestamo;
    private Long idTipoAporte;

    private Long tipoCartera;
    private Long dias;
    private Long idBanda;
    private String banda;

    private String cuentaContable;
    private String nombreCuenta;
    private Long idAsiento;

    public FilaDistribucionBanda() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getParticipe() {
        return participe;
    }

    public void setParticipe(String participe) {
        this.participe = participe;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public Long getCodigoAsoprep() {
        return codigoAsoprep;
    }

    public void setCodigoAsoprep(Long codigoAsoprep) {
        this.codigoAsoprep = codigoAsoprep;
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Long numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public LocalDate getFechaAplicacion() {
        return fechaAplicacion;
    }

    public void setFechaAplicacion(LocalDate fechaAplicacion) {
        this.fechaAplicacion = fechaAplicacion;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public Long getIdTipoPrestamo() {
        return idTipoPrestamo;
    }

    public void setIdTipoPrestamo(Long idTipoPrestamo) {
        this.idTipoPrestamo = idTipoPrestamo;
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public Long getTipoCartera() {
        return tipoCartera;
    }

    public void setTipoCartera(Long tipoCartera) {
        this.tipoCartera = tipoCartera;
    }

    public Long getDias() {
        return dias;
    }

    public void setDias(Long dias) {
        this.dias = dias;
    }

    public Long getIdBanda() {
        return idBanda;
    }

    public void setIdBanda(Long idBanda) {
        this.idBanda = idBanda;
    }

    public String getBanda() {
        return banda;
    }

    public void setBanda(String banda) {
        this.banda = banda;
    }

    public String getCuentaContable() {
        return cuentaContable;
    }

    public void setCuentaContable(String cuentaContable) {
        this.cuentaContable = cuentaContable;
    }

    public String getNombreCuenta() {
        return nombreCuenta;
    }

    public void setNombreCuenta(String nombreCuenta) {
        this.nombreCuenta = nombreCuenta;
    }

    public Long getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(Long idAsiento) {
        this.idAsiento = idAsiento;
    }
}
