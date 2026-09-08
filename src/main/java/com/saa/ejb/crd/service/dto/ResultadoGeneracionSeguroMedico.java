package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de generar el seguro médico del mes (proceso de INICIO de mes, §4.1 de
 * API-DOS-PROCESOS-MENSUALES-JUBILADOS.md). Solo fija {@code PGPCVLSG} por jubilado y genera
 * la orden agregada al proveedor — no toca pensión, cruce ni devengo, eso lo hace el proceso
 * de pensiones (fin de mes).
 *
 * Forma canónica acordada con el frontend (2026-09-07): {@code jubilados}, {@code total},
 * {@code idOrdenPago} y {@code mensaje} son los cuatro campos que consume la pantalla; el
 * resto es diagnóstico adicional que no le hace falta pero tampoco le estorba.
 *
 * @see com.saa.ejb.crd.service.PagoPensionComplementariaService#generarSeguroDelMes(Long, Integer, Integer, String)
 */
public class ResultadoGeneracionSeguroMedico {

    private Integer anio;
    private Integer mes;

    /** Jubilados evaluados (todo el padrón vigente a la fecha de ejecución). */
    private int evaluados;

    /** Canónico. Jubilados a los que se les fijó/completó PGPCVLSG en esta corrida. */
    private int jubilados;

    /** Jubilados cuya fila del período ya tenía el seguro fijado (reintento). */
    private int yaGenerados;

    private int conError;

    private List<String> errores = new ArrayList<>();

    /** Canónico. Suma de PGPCVLSG de todos los jubilados con "jubilados", redondeada. */
    private double total;

    /** Canónico. Id de la orden de pago agregada al proveedor (PGS.PGTR), o {@code null} si el total fue $0. */
    private Long idOrdenPago;

    /** Canónico. Resumen legible para la pantalla. */
    private String mensaje;

    public ResultadoGeneracionSeguroMedico() {
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

    public int getJubilados() {
        return jubilados;
    }

    public void setJubilados(int jubilados) {
        this.jubilados = jubilados;
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

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Long getIdOrdenPago() {
        return idOrdenPago;
    }

    public void setIdOrdenPago(Long idOrdenPago) {
        this.idOrdenPago = idOrdenPago;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
