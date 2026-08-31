package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de correr {@code PagoPensionComplementariaService#generarPagosDelMes} para un
 * período. Un jubilado con datos malos (sin VPPC activo, sin cuenta bancaria, saldo
 * insuficiente) no aborta el lote — se cuenta como error y el resto sigue, mismo criterio que
 * {@code DevolucionAporteService#sincronizarPagos}.
 */
public class ResultadoGeneracionPagosPension {

    private Integer anio;
    private Integer mes;

    /** Cuántos jubilados JUBILADO_COMPLEMENTARIO con VPPC activo se evaluaron */
    private int evaluados;

    /** Pagos PGPC nuevos generados en esta corrida */
    private int generados;

    /** Ya tenían PGPC para este período (idempotencia) — no es error, se informa */
    private int yaGenerados;

    private int conError;

    private List<String> errores = new ArrayList<>();

    public ResultadoGeneracionPagosPension() {
    }

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

    public int getYaGenerados() {
        return yaGenerados;
    }

    public void setYaGenerados(int yaGenerados) {
        this.yaGenerados = yaGenerados;
    }

    public int getConError() {
        return conError;
    }

    public void setConError(int conError) {
        this.conError = conError;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}
