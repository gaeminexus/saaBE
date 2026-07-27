/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * @author GaemiSoft
 * <p>DTO de resumen de una previsualizacion o confirmacion de importacion de
 * extracto bancario. No es una entidad JPA - se usa solo para la respuesta
 * de los endpoints /exbc/importar/validar y /exbc/importar/confirmar.</p>
 */
public class ResumenImportacionExtracto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idCuentaBancaria;
    private Long idPeriodo;
    private String nombrePeriodo;
    private String nombreBanco;
    private String numeroCuenta;
    private String archivoNombre;
    private String formatoDetectado;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Double saldoInicial;
    private Double saldoFinal;
    private Integer totalFilas;
    private Double totalDebito;
    private Double totalCredito;
    private List<String> advertencias;

    // Filas cuya fecha de transaccion cae fuera del primerDia/ultimoDia del
    // periodo elegido por el usuario (p.ej. corte de fin de mes) - advertencia
    // informativa, nunca bloqueante.
    private Integer totalTransaccionesFueraPeriodo;
    private List<String> transaccionesFueraPeriodo;

    private boolean archivoYaCargado;
    private Long idExtractoExistente;

    // Solo se llena en la respuesta de /confirmar (idempotencia/auditoria):
    private Long idExtractoCreado;

    public Long getIdCuentaBancaria() {
        return idCuentaBancaria;
    }

    public void setIdCuentaBancaria(Long idCuentaBancaria) {
        this.idCuentaBancaria = idCuentaBancaria;
    }

    public Long getIdPeriodo() {
        return idPeriodo;
    }

    public void setIdPeriodo(Long idPeriodo) {
        this.idPeriodo = idPeriodo;
    }

    public String getNombrePeriodo() {
        return nombrePeriodo;
    }

    public void setNombrePeriodo(String nombrePeriodo) {
        this.nombrePeriodo = nombrePeriodo;
    }

    public String getNombreBanco() {
        return nombreBanco;
    }

    public void setNombreBanco(String nombreBanco) {
        this.nombreBanco = nombreBanco;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public String getArchivoNombre() {
        return archivoNombre;
    }

    public void setArchivoNombre(String archivoNombre) {
        this.archivoNombre = archivoNombre;
    }

    public String getFormatoDetectado() {
        return formatoDetectado;
    }

    public void setFormatoDetectado(String formatoDetectado) {
        this.formatoDetectado = formatoDetectado;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Double getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(Double saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public Double getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(Double saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public Integer getTotalFilas() {
        return totalFilas;
    }

    public void setTotalFilas(Integer totalFilas) {
        this.totalFilas = totalFilas;
    }

    public Double getTotalDebito() {
        return totalDebito;
    }

    public void setTotalDebito(Double totalDebito) {
        this.totalDebito = totalDebito;
    }

    public Double getTotalCredito() {
        return totalCredito;
    }

    public void setTotalCredito(Double totalCredito) {
        this.totalCredito = totalCredito;
    }

    public List<String> getAdvertencias() {
        return advertencias;
    }

    public void setAdvertencias(List<String> advertencias) {
        this.advertencias = advertencias;
    }

    public Integer getTotalTransaccionesFueraPeriodo() {
        return totalTransaccionesFueraPeriodo;
    }

    public void setTotalTransaccionesFueraPeriodo(Integer totalTransaccionesFueraPeriodo) {
        this.totalTransaccionesFueraPeriodo = totalTransaccionesFueraPeriodo;
    }

    public List<String> getTransaccionesFueraPeriodo() {
        return transaccionesFueraPeriodo;
    }

    public void setTransaccionesFueraPeriodo(List<String> transaccionesFueraPeriodo) {
        this.transaccionesFueraPeriodo = transaccionesFueraPeriodo;
    }

    public boolean isArchivoYaCargado() {
        return archivoYaCargado;
    }

    public void setArchivoYaCargado(boolean archivoYaCargado) {
        this.archivoYaCargado = archivoYaCargado;
    }

    public Long getIdExtractoExistente() {
        return idExtractoExistente;
    }

    public void setIdExtractoExistente(Long idExtractoExistente) {
        this.idExtractoExistente = idExtractoExistente;
    }

    public Long getIdExtractoCreado() {
        return idExtractoCreado;
    }

    public void setIdExtractoCreado(Long idExtractoCreado) {
        this.idExtractoCreado = idExtractoCreado;
    }
}
