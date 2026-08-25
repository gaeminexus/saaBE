package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Resultado de clasificar una antigüedad en días dentro de la parametrización de bandas
 * de un producto: qué banda le toca y en qué cuenta contable se registra su capital.
 *
 * Es el retorno de {@code ClasificadorBandaService.clasificar} — el único punto del
 * sistema que resuelve días → banda → cuenta. Todos los procesos contables de CRD
 * (apertura/cierre, asiento de vencidos, cambio de bandas, re-bandeo por abono a capital)
 * lo consumen; ninguno reimplementa la regla.
 */
public class ResultadoClasificacionBanda {

    /** Código de la configuración usada (CRD.CBPR.CBPRCDGO). */
    private Long idConfiguracion;

    /** Código del producto consultado. */
    private Long idProducto;

    /** Código de la empresa consultada. */
    private Long idEmpresa;

    /** Tipo de cartera consultado: 1 = por vencer, 2 = vencido. */
    private Long tipoCartera;

    /** Fecha a la que se resolvió la vigencia de la configuración. */
    private LocalDate fecha;

    /** Días de antigüedad que se clasificaron. */
    private Long dias;

    /** La banda que le corresponde, con su rango derivado y su cuenta. */
    private BandaProductoDetalle banda;

    public ResultadoClasificacionBanda() {
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

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Long getDias() {
        return dias;
    }

    public void setDias(Long dias) {
        this.dias = dias;
    }

    public BandaProductoDetalle getBanda() {
        return banda;
    }

    public void setBanda(BandaProductoDetalle banda) {
        this.banda = banda;
    }
}
