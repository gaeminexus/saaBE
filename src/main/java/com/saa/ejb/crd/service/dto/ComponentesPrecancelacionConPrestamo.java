package com.saa.ejb.crd.service.dto;

/**
 * C16 (D-021) — componentes de la precancelación de un préstamo pagada con el dinero de un préstamo
 * nuevo, calculados desde {@code CRD.DTPR} a una fecha (solo lectura). Regla del cliente verificada en
 * junio 2025: capital pendiente al corte anterior + interés COMPLETO + seguro de la cuota del mes.
 */
public class ComponentesPrecancelacionConPrestamo {

    /** Tabla con cuotas futuras pendientes: capital = cuota del mes + futuras (saldos del motor). */
    public static final String TABLA_VIGENTE = "TABLA_VIGENTE";
    /** Tabla migrada (futuras saldadas sin pago o inexistentes): capital = regla de la G48 de mayo 2025. */
    public static final String TABLA_MIGRADA = "TABLA_MIGRADA";

    private Long idPrestamo;
    private Long idCuotaDelMes;
    private Double numeroCuotaDelMes;
    private Double capital;
    private Double interes;
    private Double seguro;
    private Double mora;
    private Double total;
    private String tabla;

    /**
     * Parte de DTPRTTLL de la cuota del mes que repite el capital de las cuotas futuras en 7 (patrón
     * migrado C: DTPRCPTL = capital propio, total con el futuro incluido). Se descuenta del total antes
     * de pagar la cuota para no cobrar el futuro dos veces.
     */
    private Double excesoTotalCuotaDelMes = 0.0;

    public Double getExcesoTotalCuotaDelMes() { return excesoTotalCuotaDelMes; }
    public void setExcesoTotalCuotaDelMes(Double v) { this.excesoTotalCuotaDelMes = v; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
    public Long getIdCuotaDelMes() { return idCuotaDelMes; }
    public void setIdCuotaDelMes(Long idCuotaDelMes) { this.idCuotaDelMes = idCuotaDelMes; }
    public Double getNumeroCuotaDelMes() { return numeroCuotaDelMes; }
    public void setNumeroCuotaDelMes(Double numeroCuotaDelMes) { this.numeroCuotaDelMes = numeroCuotaDelMes; }
    public Double getCapital() { return capital; }
    public void setCapital(Double capital) { this.capital = capital; }
    public Double getInteres() { return interes; }
    public void setInteres(Double interes) { this.interes = interes; }
    public Double getSeguro() { return seguro; }
    public void setSeguro(Double seguro) { this.seguro = seguro; }
    public Double getMora() { return mora; }
    public void setMora(Double mora) { this.mora = mora; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public String getTabla() { return tabla; }
    public void setTabla(String tabla) { this.tabla = tabla; }
}
