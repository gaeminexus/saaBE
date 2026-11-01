package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Resumen de {@code POST /pgpc/reprocesarPeriodo} (C20, pensión complementaria de un mes pasado). */
public class ResultadoReprocesoPensionPeriodo {

    private Integer anio;
    private Integer mes;
    /** ROL (filas de la solicitud) o PADRON (VPPC activa con traslado de jubilación al corte). */
    private String origenUniverso;
    private int evaluados;
    private int generados;
    private int yaExistian;
    private int conError;
    private int pagosConfirmados;
    private int ordenesPendientes;
    private double totalPension;
    private double totalSeguro;
    private double totalCruzado;
    private double totalNeto;
    private double totalPagadoBanco;
    private double totalExcedenteNoCruzado;
    /** Seguro de salud devengado del período sin orden al proveedor (este proceso no la genera). */
    private double seguroSinOrdenProveedor;
    private List<DetalleReprocesoPension> detalle = new ArrayList<>();
    private List<String> errores = new ArrayList<>();

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public String getOrigenUniverso() {
        return origenUniverso;
    }

    public void setOrigenUniverso(String origenUniverso) {
        this.origenUniverso = origenUniverso;
    }

    public int getEvaluados() {
        return evaluados;
    }

    public void setEvaluados(int evaluados) {
        this.evaluados = evaluados;
    }

    public int getGenerados() {
        return generados;
    }

    public void setGenerados(int generados) {
        this.generados = generados;
    }

    public int getYaExistian() {
        return yaExistian;
    }

    public void setYaExistian(int yaExistian) {
        this.yaExistian = yaExistian;
    }

    public int getConError() {
        return conError;
    }

    public void setConError(int conError) {
        this.conError = conError;
    }

    public int getPagosConfirmados() {
        return pagosConfirmados;
    }

    public void setPagosConfirmados(int pagosConfirmados) {
        this.pagosConfirmados = pagosConfirmados;
    }

    public int getOrdenesPendientes() {
        return ordenesPendientes;
    }

    public void setOrdenesPendientes(int ordenesPendientes) {
        this.ordenesPendientes = ordenesPendientes;
    }

    public double getTotalPension() {
        return totalPension;
    }

    public void setTotalPension(double totalPension) {
        this.totalPension = totalPension;
    }

    public double getTotalSeguro() {
        return totalSeguro;
    }

    public void setTotalSeguro(double totalSeguro) {
        this.totalSeguro = totalSeguro;
    }

    public double getTotalCruzado() {
        return totalCruzado;
    }

    public void setTotalCruzado(double totalCruzado) {
        this.totalCruzado = totalCruzado;
    }

    public double getTotalNeto() {
        return totalNeto;
    }

    public void setTotalNeto(double totalNeto) {
        this.totalNeto = totalNeto;
    }

    public double getTotalPagadoBanco() {
        return totalPagadoBanco;
    }

    public void setTotalPagadoBanco(double totalPagadoBanco) {
        this.totalPagadoBanco = totalPagadoBanco;
    }

    public double getTotalExcedenteNoCruzado() {
        return totalExcedenteNoCruzado;
    }

    public void setTotalExcedenteNoCruzado(double totalExcedenteNoCruzado) {
        this.totalExcedenteNoCruzado = totalExcedenteNoCruzado;
    }

    public double getSeguroSinOrdenProveedor() {
        return seguroSinOrdenProveedor;
    }

    public void setSeguroSinOrdenProveedor(double seguroSinOrdenProveedor) {
        this.seguroSinOrdenProveedor = seguroSinOrdenProveedor;
    }

    public List<DetalleReprocesoPension> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<DetalleReprocesoPension> detalle) {
        this.detalle = detalle;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}
