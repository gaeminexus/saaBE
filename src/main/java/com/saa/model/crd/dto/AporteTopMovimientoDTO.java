package com.saa.model.crd.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para top movimientos individuales de aportes
 */
public class AporteTopMovimientoDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long aporteId;
    private Long tipoAporteId;
    private String tipoAporteNombre;
    private Long entidadId;
    private String entidadNombre;
    private LocalDateTime fechaTransaccion;
    private Double valor;
    private Double magnitud;
    
    public AporteTopMovimientoDTO() {
    }
    
    public AporteTopMovimientoDTO(Long aporteId, Long tipoAporteId, String tipoAporteNombre, 
                                  Long entidadId, String entidadNombre, LocalDateTime fechaTransaccion, 
                                  Double valor, Double magnitud) {
        this.aporteId = aporteId;
        this.tipoAporteId = tipoAporteId;
        this.tipoAporteNombre = tipoAporteNombre;
        this.entidadId = entidadId;
        this.entidadNombre = entidadNombre;
        this.fechaTransaccion = fechaTransaccion;
        this.valor = valor;
        this.magnitud = magnitud;
    }

    // Getters y Setters
    public Long getAporteId() {
        return aporteId;
    }

    public void setAporteId(Long aporteId) {
        this.aporteId = aporteId;
    }

    public Long getTipoAporteId() {
        return tipoAporteId;
    }

    public void setTipoAporteId(Long tipoAporteId) {
        this.tipoAporteId = tipoAporteId;
    }

    public String getTipoAporteNombre() {
        return tipoAporteNombre;
    }

    public void setTipoAporteNombre(String tipoAporteNombre) {
        this.tipoAporteNombre = tipoAporteNombre;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public String getEntidadNombre() {
        return entidadNombre;
    }

    public void setEntidadNombre(String entidadNombre) {
        this.entidadNombre = entidadNombre;
    }

    public LocalDateTime getFechaTransaccion() {
        return fechaTransaccion;
    }

    public void setFechaTransaccion(LocalDateTime fechaTransaccion) {
        this.fechaTransaccion = fechaTransaccion;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getMagnitud() {
        return magnitud;
    }

    public void setMagnitud(Double magnitud) {
        this.magnitud = magnitud;
    }
}
