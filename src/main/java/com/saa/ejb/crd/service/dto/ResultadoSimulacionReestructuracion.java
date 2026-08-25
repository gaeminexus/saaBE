package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de {@code POST /rest/prst/simularReestructuracion}: comparativa antes/después.
 * Nada de esto se persiste (decisión 8 del plan).
 */
public class ResultadoSimulacionReestructuracion {

    private Long idPrestamo;

    /** 1 = francesa, 2 = alemana (del préstamo; la reestructuración no cambia el tipo). */
    private Long tipoAmortizacion;

    /** Suma de (capital - capitalPagado) de las cuotas pendientes hoy, sin capitalizar nada. */
    private Double saldoCapitalPendiente;

    private Boolean capitalizarVencido;

    /** 0.0 si capitalizarVencido es false. */
    private Double moraCapitalizada;

    /** 0.0 si capitalizarVencido es false. */
    private Double interesVencidoCapitalizado;

    /** Monto con el que se siembra la tabla nueva: saldoCapitalPendiente + lo capitalizado. */
    private Double capitalDeArranque;

    /** Tasa anual vigente del préstamo (PRSTTSAA), la haya pedido cambiar el usuario o no. */
    private Double tasaActual;

    /** La efectivamente usada para la tabla nueva: si nuevaTasaAnual vino null, es tasaActual. */
    private Double tasaNueva;

    /** Cantidad de cuotas pendientes hoy (lo que "mantener el plazo actual" significa acá). */
    private Long plazoActual;
    private Long plazoNuevo;

    private Double cuotaActual;
    private Double cuotaNueva;

    /** Eco del `mesesGracia` pedido en la solicitud (0 o 1; ver decisión 4 de la §11.8). */
    private Integer mesesGracia;

    /**
     * Lo que el préstamo actual cobraría en total de acá en adelante: suma de {@code total} de
     * las cuotas pendientes MÁS la mora y el interés vencido pendientes (los deba el préstamo o
     * no se capitalicen: si no se capitalizan, siguen siendo una deuda aparte, no desaparecen).
     */
    private Double totalAPagarActual;

    /**
     * Suma de {@code total} de la tabla nueva. Si capitalizarVencido es false, NO incluye la mora
     * ni el interés vencido pendientes: esos quedan fuera de la tabla nueva y siguen debiéndose.
     */
    private Double totalAPagarNuevo;

    private List<CuotaProyectada> tablaProyectada = new ArrayList<>();

    public ResultadoSimulacionReestructuracion() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getTipoAmortizacion() {
        return tipoAmortizacion;
    }

    public void setTipoAmortizacion(Long tipoAmortizacion) {
        this.tipoAmortizacion = tipoAmortizacion;
    }

    public Double getSaldoCapitalPendiente() {
        return saldoCapitalPendiente;
    }

    public void setSaldoCapitalPendiente(Double saldoCapitalPendiente) {
        this.saldoCapitalPendiente = saldoCapitalPendiente;
    }

    public Boolean getCapitalizarVencido() {
        return capitalizarVencido;
    }

    public void setCapitalizarVencido(Boolean capitalizarVencido) {
        this.capitalizarVencido = capitalizarVencido;
    }

    public Double getMoraCapitalizada() {
        return moraCapitalizada;
    }

    public void setMoraCapitalizada(Double moraCapitalizada) {
        this.moraCapitalizada = moraCapitalizada;
    }

    public Double getInteresVencidoCapitalizado() {
        return interesVencidoCapitalizado;
    }

    public void setInteresVencidoCapitalizado(Double interesVencidoCapitalizado) {
        this.interesVencidoCapitalizado = interesVencidoCapitalizado;
    }

    public Double getCapitalDeArranque() {
        return capitalDeArranque;
    }

    public void setCapitalDeArranque(Double capitalDeArranque) {
        this.capitalDeArranque = capitalDeArranque;
    }

    public Double getTasaActual() {
        return tasaActual;
    }

    public void setTasaActual(Double tasaActual) {
        this.tasaActual = tasaActual;
    }

    public Double getTasaNueva() {
        return tasaNueva;
    }

    public void setTasaNueva(Double tasaNueva) {
        this.tasaNueva = tasaNueva;
    }

    public Long getPlazoActual() {
        return plazoActual;
    }

    public void setPlazoActual(Long plazoActual) {
        this.plazoActual = plazoActual;
    }

    public Long getPlazoNuevo() {
        return plazoNuevo;
    }

    public void setPlazoNuevo(Long plazoNuevo) {
        this.plazoNuevo = plazoNuevo;
    }

    public Double getCuotaActual() {
        return cuotaActual;
    }

    public void setCuotaActual(Double cuotaActual) {
        this.cuotaActual = cuotaActual;
    }

    public Double getCuotaNueva() {
        return cuotaNueva;
    }

    public void setCuotaNueva(Double cuotaNueva) {
        this.cuotaNueva = cuotaNueva;
    }

    public Integer getMesesGracia() {
        return mesesGracia;
    }

    public void setMesesGracia(Integer mesesGracia) {
        this.mesesGracia = mesesGracia;
    }

    public Double getTotalAPagarActual() {
        return totalAPagarActual;
    }

    public void setTotalAPagarActual(Double totalAPagarActual) {
        this.totalAPagarActual = totalAPagarActual;
    }

    public Double getTotalAPagarNuevo() {
        return totalAPagarNuevo;
    }

    public void setTotalAPagarNuevo(Double totalAPagarNuevo) {
        this.totalAPagarNuevo = totalAPagarNuevo;
    }

    public List<CuotaProyectada> getTablaProyectada() {
        return tablaProyectada;
    }

    public void setTablaProyectada(List<CuotaProyectada> tablaProyectada) {
        this.tablaProyectada = tablaProyectada;
    }
}
