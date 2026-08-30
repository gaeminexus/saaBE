package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de {@code GET /rest/asgn/estadoContable/{idCarga}} — CONTRATO CONGELADO, ver
 * {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md} §2.4. {@code asientos} vacío
 * es un resultado VÁLIDO (todavía no se contabilizó nada), no un error.
 */
public class EstadoContablePetro {

    private Long idCarga;
    private Boolean contabilidadActiva;
    private List<AsientoPetroDTO> asientos = new ArrayList<>();

    public Long getIdCarga() {
        return idCarga;
    }

    public void setIdCarga(Long idCarga) {
        this.idCarga = idCarga;
    }

    public Boolean getContabilidadActiva() {
        return contabilidadActiva;
    }

    public void setContabilidadActiva(Boolean contabilidadActiva) {
        this.contabilidadActiva = contabilidadActiva;
    }

    public List<AsientoPetroDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoPetroDTO> asientos) {
        this.asientos = asientos;
    }
}
