package com.saa.ws.movil.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Espejo movil de {@code EstadoCuentaAportesDTO} (ver esa clase) para
 * {@code GET /movil/aportes/{idEntidad}/movimientos}.
 *
 * <p>{@link #rangoPorDefectoAplicado}, {@link #desdeAplicado} y {@link #hastaAplicado}: cuando el
 * borde no manda {@code desde}/{@code hasta}, {@code /movil} aplica un rango por defecto de los
 * últimos 24 meses y lo declara acá (§5.2 del contrato) — el borde no tiene que adivinar qué
 * rango se usó.</p>
 */
public class EstadoCuentaAportesMovilDTO {

    private Long idEntidad;
    private String identificacion;
    private String razonSocial;
    private List<PeriodoEstadoCuentaMovilDTO> periodos = new ArrayList<>();
    private Double totalFaltante;
    private boolean rangoPorDefectoAplicado;
    private String desdeAplicado;
    private String hastaAplicado;

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public List<PeriodoEstadoCuentaMovilDTO> getPeriodos() {
        return periodos;
    }

    public void setPeriodos(List<PeriodoEstadoCuentaMovilDTO> periodos) {
        this.periodos = periodos;
    }

    public Double getTotalFaltante() {
        return totalFaltante;
    }

    public void setTotalFaltante(Double totalFaltante) {
        this.totalFaltante = totalFaltante;
    }

    public boolean isRangoPorDefectoAplicado() {
        return rangoPorDefectoAplicado;
    }

    public void setRangoPorDefectoAplicado(boolean rangoPorDefectoAplicado) {
        this.rangoPorDefectoAplicado = rangoPorDefectoAplicado;
    }

    public String getDesdeAplicado() {
        return desdeAplicado;
    }

    public void setDesdeAplicado(String desdeAplicado) {
        this.desdeAplicado = desdeAplicado;
    }

    public String getHastaAplicado() {
        return hastaAplicado;
    }

    public void setHastaAplicado(String hastaAplicado) {
        this.hastaAplicado = hastaAplicado;
    }
}
