package com.saa.ejb.crd.service.dto;

/**
 * Una cuenta contable candidata a ser la cuenta de una banda, para el buscador de la
 * pantalla de parametrización.
 *
 * Es un DTO delgado a propósito: el plan de cuentas de la empresa tiene más de mil filas y
 * la entidad {@code PlanCuenta} arrastra naturaleza y empresa completas en cada una.
 * Solo se ofrecen cuentas ACTIVAS y de MOVIMIENTO — una cuenta de acumulación no puede
 * recibir saldo.
 */
public class CuentaBandaDisponible {

    /** Código de la cuenta (CNT.PLNN.PLNNCDGO). Es lo que se manda en {@code idPlanCuenta}. */
    private Long idPlanCuenta;

    /** Cuenta contable con puntos, p.ej. "1.3.01.05". */
    private String cuentaContable;

    /** Nombre de la cuenta, p.ej. "DE 1 A 30 DIAS". */
    private String nombre;

    public CuentaBandaDisponible() {
    }

    public CuentaBandaDisponible(Long idPlanCuenta, String cuentaContable, String nombre) {
        this.idPlanCuenta = idPlanCuenta;
        this.cuentaContable = cuentaContable;
        this.nombre = nombre;
    }

    public Long getIdPlanCuenta() {
        return idPlanCuenta;
    }

    public void setIdPlanCuenta(Long idPlanCuenta) {
        this.idPlanCuenta = idPlanCuenta;
    }

    public String getCuentaContable() {
        return cuentaContable;
    }

    public void setCuentaContable(String cuentaContable) {
        this.cuentaContable = cuentaContable;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
