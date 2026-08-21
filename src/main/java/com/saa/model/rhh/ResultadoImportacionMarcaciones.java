package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de la importacion de un archivo de marcaciones del biometrico.
 *
 * <p>POJO de transporte, sin @Entity. Sigue el precedente de
 * {@code com.saa.model.cnt.RespuestaBalance}. El frontend lo espeja como interface
 * en {@code modules/rrh/model/}.</p>
 */
@SuppressWarnings("serial")
public class ResultadoImportacionMarcaciones implements Serializable {

    private Long idCarga;

    private String nombreArchivo;

    private Integer lineasTotales;

    private Integer lineasOk;

    private Integer lineasError;

    private Integer lineasDuplicadas;

    private List<String> errores = new ArrayList<>();

    private LocalDate fechaDesde;

    private LocalDate fechaHasta;

    public Long getIdCarga() {
        return idCarga;
    }

    public void setIdCarga(Long idCarga) {
        this.idCarga = idCarga;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public Integer getLineasTotales() {
        return lineasTotales;
    }

    public void setLineasTotales(Integer lineasTotales) {
        this.lineasTotales = lineasTotales;
    }

    public Integer getLineasOk() {
        return lineasOk;
    }

    public void setLineasOk(Integer lineasOk) {
        this.lineasOk = lineasOk;
    }

    public Integer getLineasError() {
        return lineasError;
    }

    public void setLineasError(Integer lineasError) {
        this.lineasError = lineasError;
    }

    public Integer getLineasDuplicadas() {
        return lineasDuplicadas;
    }

    public void setLineasDuplicadas(Integer lineasDuplicadas) {
        this.lineasDuplicadas = lineasDuplicadas;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
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
}
