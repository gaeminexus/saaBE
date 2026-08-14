package com.saa.ejb.crd.service.dto;

/**
 * Saldo disponible de un tipo de aporte para una entidad (partícipe).
 *
 * El saldo ES la suma neta de {@code APRT.APRTVLRR}: los pagos con aportes se registran como
 * filas NEGATIVAS, de modo que la suma refleja siempre lo disponible. Los campos
 * {@code valorPagado}/{@code saldo} de APRT tienen OTRA semántica (son la mecánica del FIFO
 * del proceso Petro) y no intervienen aquí.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.2.
 */
public class SaldoTipoAporte {

    private Long idTipoAporte;
    private String nombre;
    private Double saldo;

    public SaldoTipoAporte() {
    }

    public SaldoTipoAporte(Long idTipoAporte, String nombre, Double saldo) {
        this.idTipoAporte = idTipoAporte;
        this.nombre = nombre;
        this.saldo = saldo;
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }
}
