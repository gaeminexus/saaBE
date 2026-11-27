package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** C51: lo que bloquearía hoy el cierre contable de un período ({@code GET /prdo/candado/{idEmpresa}/{anio}/{mes}}). Solo lectura. */
public class ResultadoCandado implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idEmpresa;
    private Long anio;
    private Long mes;
    /** Propiedad {@code saa.cierres.candadoCierre} encendida. */
    private boolean candadoActivo;
    /** Hay al menos una falla sin cubrir: con el candado encendido, {@code /prdo/cerrar} se rechaza. */
    private boolean bloquearia;
    private List<FallaCandado> fallas = new ArrayList<FallaCandado>();

    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
    public Long getAnio() { return anio; }
    public void setAnio(Long anio) { this.anio = anio; }
    public Long getMes() { return mes; }
    public void setMes(Long mes) { this.mes = mes; }
    public boolean isCandadoActivo() { return candadoActivo; }
    public void setCandadoActivo(boolean candadoActivo) { this.candadoActivo = candadoActivo; }
    public boolean isBloquearia() { return bloquearia; }
    public void setBloquearia(boolean bloquearia) { this.bloquearia = bloquearia; }
    public List<FallaCandado> getFallas() { return fallas; }
    public void setFallas(List<FallaCandado> fallas) { this.fallas = fallas; }
}
