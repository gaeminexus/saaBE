package com.saa.ejb.crd.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Un descuento de préstamo del rol de pensiones de un mes pasado (C20,
 * {@code POST /pgpc/reprocesarPeriodo}). Se aplica como cobro con aportes de pensión complementaria
 * (tipo 23) contra el préstamo, con la fecha del período. Campos de más del lote (operación, número
 * de cuota, estado de hoy...) se ignoran.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CruceReprocesoPension {

    /** Código de producto del rol (PQ, PH, PE...). Informativo. */
    private String producto;

    /**
     * Valor que el rol descontó para este préstamo. Si viene {@code null} (solo en el modo padrón,
     * sin filas), se cruza lo exigible al corte.
     */
    private Double valorRol;

    /** Préstamo del SAA ({@code CRD.PRST.PRSTCDGO}). Sin préstamo el descuento queda como novedad. */
    private Long idPrestamo;

    /** Cuota del mes que el rol pagó ({@code CRD.DTPR.DTPRCDGO}). Informativo, para el control. */
    @JsonAlias({ "idCuotaJunio", "idCuotaMes" })
    private Long idCuota;

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public Double getValorRol() {
        return valorRol;
    }

    public void setValorRol(Double valorRol) {
        this.valorRol = valorRol;
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getIdCuota() {
        return idCuota;
    }

    public void setIdCuota(Long idCuota) {
        this.idCuota = idCuota;
    }
}
