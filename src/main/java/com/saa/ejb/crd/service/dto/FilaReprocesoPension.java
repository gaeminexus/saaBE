package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Un jubilado del rol de pensiones de un mes pasado (C20, {@code POST /pgpc/reprocesarPeriodo}).
 * Mismo formato que {@code filas[]} de los lotes del expediente (p. ej.
 * {@code 14-lote-pgpc-junio-2025.json}); los campos de más se ignoran.
 *
 * <p>Los valores del ROL mandan sobre la configuración de hoy (VPPC): el rol es el documento.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilaReprocesoPension {

    private Long idEntidad;

    /** Pensión BRUTA del rol (incluye el seguro de salud). Sin ella se usa la VPPC activa. */
    private Double pension;

    /** Seguro de salud descontado en el rol (parte de {@link #pension}). */
    private Double seguroSalud;

    /** Total de descuentos de préstamos del rol. Informativo: lo que se cruza sale de {@link #cruces}. */
    private Double descuentoPrestamos;

    /** Neto del rol (pensión − seguro − descuentos). Tope de lo que se paga al jubilado. */
    private Double netoRol;

    /** Etiqueta de pago del lote (LOTE_..., TRANSFERENCIA_..., PENDIENTE_...). Informativa. */
    private String pago;

    /**
     * Fecha del débito en el extracto. Con fecha y cuenta, la orden nace CONFIRMADA con su egreso
     * en esa fecha; sin fecha queda pendiente de aprobación en tesorería.
     */
    private LocalDate fechaPagoBanco;

    /** Cuenta bancaria propia (TSR.CNBC) del débito. Si falta se usa la de la solicitud. */
    private Long idCuentaBancariaPago;

    /** Referencia del débito (p. ej. Ru-858739). Si falta se usa la de la solicitud. */
    private String referenciaPago;

    private String notaPago;

    private List<CruceReprocesoPension> cruces = new ArrayList<>();

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Double getPension() {
        return pension;
    }

    public void setPension(Double pension) {
        this.pension = pension;
    }

    public Double getSeguroSalud() {
        return seguroSalud;
    }

    public void setSeguroSalud(Double seguroSalud) {
        this.seguroSalud = seguroSalud;
    }

    public Double getDescuentoPrestamos() {
        return descuentoPrestamos;
    }

    public void setDescuentoPrestamos(Double descuentoPrestamos) {
        this.descuentoPrestamos = descuentoPrestamos;
    }

    public Double getNetoRol() {
        return netoRol;
    }

    public void setNetoRol(Double netoRol) {
        this.netoRol = netoRol;
    }

    public String getPago() {
        return pago;
    }

    public void setPago(String pago) {
        this.pago = pago;
    }

    public LocalDate getFechaPagoBanco() {
        return fechaPagoBanco;
    }

    public void setFechaPagoBanco(LocalDate fechaPagoBanco) {
        this.fechaPagoBanco = fechaPagoBanco;
    }

    public Long getIdCuentaBancariaPago() {
        return idCuentaBancariaPago;
    }

    public void setIdCuentaBancariaPago(Long idCuentaBancariaPago) {
        this.idCuentaBancariaPago = idCuentaBancariaPago;
    }

    public String getReferenciaPago() {
        return referenciaPago;
    }

    public void setReferenciaPago(String referenciaPago) {
        this.referenciaPago = referenciaPago;
    }

    public String getNotaPago() {
        return notaPago;
    }

    public void setNotaPago(String notaPago) {
        this.notaPago = notaPago;
    }

    public List<CruceReprocesoPension> getCruces() {
        return cruces;
    }

    public void setCruces(List<CruceReprocesoPension> cruces) {
        this.cruces = cruces;
    }
}
