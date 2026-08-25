package com.saa.ejb.cxp.service.dto;

/**
 * Beneficiario de un pago que NO existe en el maestro de titulares de tesorería
 * (TSR.TTLR) ni tiene cuenta registrada en TSR.CTBN.
 *
 * Es una capacidad genérica de CXP —"pagarle a alguien que no está en el maestro"— y no
 * está ligada a ningún módulo concreto: quien arma este DTO es el proceso que origina el
 * pago, que sí conoce a su propio beneficiario.
 *
 * Los datos viajan DENORMALIZADOS a PGS.PGTR (columnas PGTRBF*) y se usan en el archivo
 * del banco cuando {@code PagoProgramado.cuentaDestino} es null. La alternativa —crear un
 * Titular por cada beneficiario ocasional— metería datos de otros módulos dentro de TSR.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class BeneficiarioOcasional {

    /** Nombre o razón social del beneficiario. Obligatorio. */
    private String nombre;

    /** Número de identificación (cédula o RUC). Obligatorio. */
    private String identificacion;

    /**
     * Banco externo al que se transfiere (TSR.BEXT).
     * Obligatorio salvo en débito automático, que no genera archivo de transferencias.
     */
    private Long idBancoExterno;

    /**
     * Tipo de cuenta: codigoAlterno del DetalleRubro del rubro de tipo de cuenta bancaria.
     */
    private Long tipoCuenta;

    /**
     * Número de cuenta destino.
     * Obligatorio salvo en débito automático.
     */
    private String numeroCuenta;

    public BeneficiarioOcasional() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public Long getIdBancoExterno() {
        return idBancoExterno;
    }

    public void setIdBancoExterno(Long idBancoExterno) {
        this.idBancoExterno = idBancoExterno;
    }

    public Long getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(Long tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }
}
