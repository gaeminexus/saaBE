package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;

/** C52: una cuenta de movimiento con saldo contrario a su naturaleza al corte del mes. Solo lectura. */
public class SaldoContrarioCuenta implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cuenta;
    private String nombre;
    /** Naturaleza esperada: DEUDORA o ACREEDORA. */
    private String naturaleza;
    /** Debe menos haber (acumulado al corte; en resultados, acumulado del anio sin el cierre del ejercicio). */
    private double saldo;
    /** Valor positivo en que el saldo es contrario. */
    private double valorContrario;
    /** BALANCE o RESULTADOS. */
    private String tipo;
    /** {@code true} si la regulariza sola POST /prdo/regularizarRedondeos (cuentas de redondeo de la plantilla). */
    private boolean regularizableAutomatico;
    /** Asiento sugerido (NO se graba): lo registra el usuario con aprobacion y novedad C25. */
    private String asientoSugerido;

    public String getCuenta() { return cuenta; }
    public void setCuenta(String cuenta) { this.cuenta = cuenta; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNaturaleza() { return naturaleza; }
    public void setNaturaleza(String naturaleza) { this.naturaleza = naturaleza; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public double getValorContrario() { return valorContrario; }
    public void setValorContrario(double valorContrario) { this.valorContrario = valorContrario; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public boolean isRegularizableAutomatico() { return regularizableAutomatico; }
    public void setRegularizableAutomatico(boolean regularizableAutomatico) { this.regularizableAutomatico = regularizableAutomatico; }
    public String getAsientoSugerido() { return asientoSugerido; }
    public void setAsientoSugerido(String asientoSugerido) { this.asientoSugerido = asientoSugerido; }
}
