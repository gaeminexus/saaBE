package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuración de bandas de un producto para un tipo de cartera, con sus bandas y los
 * rangos en días ya derivados. Es la unidad que consume la pantalla de parametrización y
 * la que devuelven los servicios de consulta.
 *
 * Se devuelve el DTO y no la entidad a propósito: la entidad arrastraría el Producto, la
 * Empresa, la Jerarquía y el PlanCuenta completos en cada banda, y no trae los rangos
 * derivados, que son justamente lo que el consumidor necesita.
 */
public class ConfiguracionBandaDetalle {

    /** Código de la configuración (CRD.CBPR.CBPRCDGO). */
    private Long idConfiguracion;

    /** Código del producto (CRD.PRDC.PRDCCDGO). */
    private Long idProducto;

    /** Nombre del producto. */
    private String nombreProducto;

    /** Código de la empresa (SCP.PJRQ.PJRQCDGO). */
    private Long idEmpresa;

    /** Tipo de cartera: 1 = por vencer, 2 = vencido. */
    private Long tipoCartera;

    /** Etiqueta del tipo de cartera: "POR VENCER" / "VENCIDO". */
    private String nombreTipoCartera;

    /** Inicio de vigencia. */
    private LocalDate fechaDesde;

    /** Fin de vigencia. NULO = configuración vigente (sin cierre). */
    private LocalDate fechaHasta;

    /**
     * {@code true} si la vigencia todavía NO empezó a la fecha de consulta.
     * Solo una configuración así puede editarse en el lugar; una ya vigente se cambia
     * cerrando su vigencia y abriendo otra.
     */
    private Boolean editable;

    /** Estado de la configuración: 1 = activo. */
    private Long estado;

    /** Bandas ordenadas por número, con el rango en días derivado. */
    private List<BandaProductoDetalle> bandas = new ArrayList<BandaProductoDetalle>();

    public ConfiguracionBandaDetalle() {
    }

    public Long getIdConfiguracion() {
        return idConfiguracion;
    }

    public void setIdConfiguracion(Long idConfiguracion) {
        this.idConfiguracion = idConfiguracion;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getTipoCartera() {
        return tipoCartera;
    }

    public void setTipoCartera(Long tipoCartera) {
        this.tipoCartera = tipoCartera;
    }

    public String getNombreTipoCartera() {
        return nombreTipoCartera;
    }

    public void setNombreTipoCartera(String nombreTipoCartera) {
        this.nombreTipoCartera = nombreTipoCartera;
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

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public List<BandaProductoDetalle> getBandas() {
        return bandas;
    }

    public void setBandas(List<BandaProductoDetalle> bandas) {
        this.bandas = bandas;
    }
}
