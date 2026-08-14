package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de una corrida del proceso diario de interés de mora.
 */
public class ResultadoCalculoMora {

    /** Fecha con la que se calculó la mora (sysdate en la corrida nocturna) */
    private LocalDate fechaCorte;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Long duracionMs;

    /** Préstamos con al menos una cuota vencida impaga */
    private Integer prestamosEvaluados;

    /** Préstamos procesados sin error */
    private Integer prestamosProcesados;

    /** Cuotas a las que se les escribió la mora */
    private Integer cuotasActualizadas;

    /** Cuotas que pasaron a EN_MORA (5) en esta corrida */
    private Integer cuotasMarcadasEnMora;

    /** Préstamos que pasaron a EN_MORA (11) en esta corrida */
    private Integer prestamosMarcadosEnMora;

    /** Préstamos EN_MORA que volvieron a VIGENTE (2) por no tener ya cuotas vencidas */
    private Integer prestamosRegularizados;

    /** Suma de la mora calculada en toda la corrida */
    private Double totalMoraCalculada;

    /** Préstamos que fallaron; su error no aborta el resto del lote */
    private Integer prestamosConError;

    /** Detalle de los errores (hasta 50) */
    private List<String> errores = new ArrayList<>();

    public ResultadoCalculoMora() {
    }

    public LocalDate getFechaCorte() {
        return fechaCorte;
    }

    public void setFechaCorte(LocalDate fechaCorte) {
        this.fechaCorte = fechaCorte;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Long getDuracionMs() {
        return duracionMs;
    }

    public void setDuracionMs(Long duracionMs) {
        this.duracionMs = duracionMs;
    }

    public Integer getPrestamosEvaluados() {
        return prestamosEvaluados;
    }

    public void setPrestamosEvaluados(Integer prestamosEvaluados) {
        this.prestamosEvaluados = prestamosEvaluados;
    }

    public Integer getPrestamosProcesados() {
        return prestamosProcesados;
    }

    public void setPrestamosProcesados(Integer prestamosProcesados) {
        this.prestamosProcesados = prestamosProcesados;
    }

    public Integer getCuotasActualizadas() {
        return cuotasActualizadas;
    }

    public void setCuotasActualizadas(Integer cuotasActualizadas) {
        this.cuotasActualizadas = cuotasActualizadas;
    }

    public Integer getCuotasMarcadasEnMora() {
        return cuotasMarcadasEnMora;
    }

    public void setCuotasMarcadasEnMora(Integer cuotasMarcadasEnMora) {
        this.cuotasMarcadasEnMora = cuotasMarcadasEnMora;
    }

    public Integer getPrestamosMarcadosEnMora() {
        return prestamosMarcadosEnMora;
    }

    public void setPrestamosMarcadosEnMora(Integer prestamosMarcadosEnMora) {
        this.prestamosMarcadosEnMora = prestamosMarcadosEnMora;
    }

    public Integer getPrestamosRegularizados() {
        return prestamosRegularizados;
    }

    public void setPrestamosRegularizados(Integer prestamosRegularizados) {
        this.prestamosRegularizados = prestamosRegularizados;
    }

    public Double getTotalMoraCalculada() {
        return totalMoraCalculada;
    }

    public void setTotalMoraCalculada(Double totalMoraCalculada) {
        this.totalMoraCalculada = totalMoraCalculada;
    }

    public Integer getPrestamosConError() {
        return prestamosConError;
    }

    public void setPrestamosConError(Integer prestamosConError) {
        this.prestamosConError = prestamosConError;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}
