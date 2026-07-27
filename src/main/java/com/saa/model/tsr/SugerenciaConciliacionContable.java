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
 * <p>DTO de una coincidencia sugerida automáticamente entre filas de
 * DetalleExtractoBancario y DetalleAsiento, pendiente de confirmación del
 * usuario. No es una entidad JPA - no se persiste como tal hasta que el
 * usuario confirma (lo cual crea un {@link GrupoConciliacionContable} real
 * vía ConciliacionContableMatchService.conciliarGrupo con estos mismos ids).</p>
 */
public class SugerenciaConciliacionContable implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> idsDetalleExtracto;
    private List<Long> idsDetalleAsiento;
    private Double valorExtracto;
    private Double valorAsiento;
    private LocalDate fechaMinima;
    private LocalDate fechaMaxima;
    private String descripcionResumen;

    public List<Long> getIdsDetalleExtracto() {
        return idsDetalleExtracto;
    }

    public void setIdsDetalleExtracto(List<Long> idsDetalleExtracto) {
        this.idsDetalleExtracto = idsDetalleExtracto;
    }

    public List<Long> getIdsDetalleAsiento() {
        return idsDetalleAsiento;
    }

    public void setIdsDetalleAsiento(List<Long> idsDetalleAsiento) {
        this.idsDetalleAsiento = idsDetalleAsiento;
    }

    public Double getValorExtracto() {
        return valorExtracto;
    }

    public void setValorExtracto(Double valorExtracto) {
        this.valorExtracto = valorExtracto;
    }

    public Double getValorAsiento() {
        return valorAsiento;
    }

    public void setValorAsiento(Double valorAsiento) {
        this.valorAsiento = valorAsiento;
    }

    public LocalDate getFechaMinima() {
        return fechaMinima;
    }

    public void setFechaMinima(LocalDate fechaMinima) {
        this.fechaMinima = fechaMinima;
    }

    public LocalDate getFechaMaxima() {
        return fechaMaxima;
    }

    public void setFechaMaxima(LocalDate fechaMaxima) {
        this.fechaMaxima = fechaMaxima;
    }

    public String getDescripcionResumen() {
        return descripcionResumen;
    }

    public void setDescripcionResumen(String descripcionResumen) {
        this.descripcionResumen = descripcionResumen;
    }
}
