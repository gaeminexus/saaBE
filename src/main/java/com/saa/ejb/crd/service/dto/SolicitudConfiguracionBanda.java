package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Guardado de una configuración de bandas COMPLETA: cabecera más la lista de bandas, en
 * una sola transacción. No hay forma de grabar una cabecera sin bandas ni de agregar
 * bandas sueltas por el flujo de negocio — una configuración a medias es una
 * configuración inválida.
 *
 * <b>Alta vs. edición.</b> Con {@code idConfiguracion} nulo se crea. Con
 * {@code idConfiguracion} presente se edita EN EL LUGAR, y solo si su vigencia todavía no
 * empezó; una configuración ya vigente se cambia con
 * {@code ConfiguracionBandaProductoService.cerrarVigencia}, que cierra la vieja y abre la
 * nueva. Mutar bandas en caliente dejaría los saldos contabilizados sin explicación.
 */
public class SolicitudConfiguracionBanda {

    /** Código de la configuración a editar. NULO = alta. */
    private Long idConfiguracion;

    /** Código del producto (CRD.PRDC). Obligatorio en el alta. */
    private Long idProducto;

    /** Código de la empresa (SCP.PJRQ). Obligatorio en el alta. */
    private Long idEmpresa;

    /** Tipo de cartera: 1 = por vencer, 2 = vencido. Obligatorio en el alta. */
    private Long tipoCartera;

    /** Inicio de vigencia. Obligatorio. */
    private LocalDate fechaDesde;

    /** Fin de vigencia. NULO = vigencia abierta, que es lo normal. */
    private LocalDate fechaHasta;

    /** Usuario que ejecuta la operación, para la auditoría. */
    private String usuario;

    /** IP desde la que se ejecuta, para la auditoría. */
    private String ip;

    /** Bandas de la configuración. Obligatoria y no vacía. */
    private List<SolicitudBanda> bandas = new ArrayList<SolicitudBanda>();

    public SolicitudConfiguracionBanda() {
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

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<SolicitudBanda> getBandas() {
        return bandas;
    }

    public void setBandas(List<SolicitudBanda> bandas) {
        this.bandas = bandas;
    }
}
