/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.parser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.model.tsr.DetalleExtractoBancario;

/**
 * @author GaemiSoft
 * <p>Resultado de parsear un estado de cuenta bancario, antes de persistir.
 * No es una entidad JPA - es el resultado intermedio que usa
 * ImportacionExtractoBancarioService tanto para la previsualizacion
 * (validar) como para la confirmacion (confirmar).</p>
 */
public class ParsedStatement {

    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Double saldoInicial;
    private Double saldoFinal;
    private String formatoDetectado;
    private List<DetalleExtractoBancario> detalles = new ArrayList<>();
    private List<String> advertencias = new ArrayList<>();

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

    public String getFormatoDetectado() {
        return formatoDetectado;
    }

    public void setFormatoDetectado(String formatoDetectado) {
        this.formatoDetectado = formatoDetectado;
    }

    public List<DetalleExtractoBancario> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleExtractoBancario> detalles) {
        this.detalles = detalles;
    }

    public List<String> getAdvertencias() {
        return advertencias;
    }

    public void setAdvertencias(List<String> advertencias) {
        this.advertencias = advertencias;
    }
}
