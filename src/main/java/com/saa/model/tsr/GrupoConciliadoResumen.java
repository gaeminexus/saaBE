package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author GaemiSoft
 * <p>Fila informativa de "lo conciliado del mes" en la preparación del cierre de partidas en
 * tránsito - proyección de GrupoConciliacionContable, no la entidad completa.</p>
 */
public class GrupoConciliadoResumen implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idGrupo;
    private Double valorExtracto;
    private Double valorAsiento;
    private LocalDateTime fechaConciliacion;
    private String usuarioConcilia;

    public Long getIdGrupo() { return idGrupo; }
    public void setIdGrupo(Long idGrupo) { this.idGrupo = idGrupo; }

    public Double getValorExtracto() { return valorExtracto; }
    public void setValorExtracto(Double valorExtracto) { this.valorExtracto = valorExtracto; }

    public Double getValorAsiento() { return valorAsiento; }
    public void setValorAsiento(Double valorAsiento) { this.valorAsiento = valorAsiento; }

    public LocalDateTime getFechaConciliacion() { return fechaConciliacion; }
    public void setFechaConciliacion(LocalDateTime fechaConciliacion) { this.fechaConciliacion = fechaConciliacion; }

    public String getUsuarioConcilia() { return usuarioConcilia; }
    public void setUsuarioConcilia(String usuarioConcilia) { this.usuarioConcilia = usuarioConcilia; }
}
