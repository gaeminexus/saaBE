package com.saa.ejb.crd.service.dto;

/**
 * Una cuota a reclasificar de banda por cobro tardío (ÍTEM 5, §6.2/§7bis, tipo 8) — el
 * llamador ({@code CobroCreditoServiceImpl}) ya resolvió las dos clasificaciones (nunca las
 * resuelve {@code ProvisionInteresService}, que solo arma el asiento y el MVIC):
 * <ul>
 *   <li>{@code bandaUltimoCierre}: la que el último cierre EJECUTADO (antes de la fecha de
 *       afectación del cobro) le dio a esta cuota, con {@code tipoCarteraYDias(fechaVencimiento,
 *       corte de esa corrida)} y la configuración vigente a la {@code fechaProceso} de esa
 *       corrida — nunca la de hoy;</li>
 *   <li>{@code bandaFechaPago}: la MISMA clasificación que ya usó {@code haberDesdePagos}/
 *       {@code registrarDistribucionPorPagos} para este pago — reusada, nunca recalculada.</li>
 * </ul>
 */
public class ItemReclasificacionBanda {

    private Long idCuota;
    private Long idPrestamo;
    private double capital;
    private ResultadoClasificacionBanda bandaUltimoCierre;
    private ResultadoClasificacionBanda bandaFechaPago;

    public Long getIdCuota() { return idCuota; }
    public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public double getCapital() { return capital; }
    public void setCapital(double capital) { this.capital = capital; }

    public ResultadoClasificacionBanda getBandaUltimoCierre() { return bandaUltimoCierre; }
    public void setBandaUltimoCierre(ResultadoClasificacionBanda bandaUltimoCierre) {
        this.bandaUltimoCierre = bandaUltimoCierre;
    }

    public ResultadoClasificacionBanda getBandaFechaPago() { return bandaFechaPago; }
    public void setBandaFechaPago(ResultadoClasificacionBanda bandaFechaPago) {
        this.bandaFechaPago = bandaFechaPago;
    }
}
