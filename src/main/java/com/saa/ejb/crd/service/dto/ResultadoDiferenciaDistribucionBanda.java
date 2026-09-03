package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Respuesta de {@code GET /rest/dsbn/diferencia} — API-AUDITORIA-BANDAS.md §4. */
public class ResultadoDiferenciaDistribucionBanda {

    private String origen;
    private Long idOrigen;
    private double diferenciaTotal;
    private int participesConDiferencia;
    private int recibieronDeMas;
    private int recibieronDeMenos;
    private List<DiferenciaParticipeDistribucionBanda> detalle = new ArrayList<>();

    public ResultadoDiferenciaDistribucionBanda() {
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Long getIdOrigen() {
        return idOrigen;
    }

    public void setIdOrigen(Long idOrigen) {
        this.idOrigen = idOrigen;
    }

    public double getDiferenciaTotal() {
        return diferenciaTotal;
    }

    public void setDiferenciaTotal(double diferenciaTotal) {
        this.diferenciaTotal = diferenciaTotal;
    }

    public int getParticipesConDiferencia() {
        return participesConDiferencia;
    }

    public void setParticipesConDiferencia(int participesConDiferencia) {
        this.participesConDiferencia = participesConDiferencia;
    }

    public int getRecibieronDeMas() {
        return recibieronDeMas;
    }

    public void setRecibieronDeMas(int recibieronDeMas) {
        this.recibieronDeMas = recibieronDeMas;
    }

    public int getRecibieronDeMenos() {
        return recibieronDeMenos;
    }

    public void setRecibieronDeMenos(int recibieronDeMenos) {
        this.recibieronDeMenos = recibieronDeMenos;
    }

    public List<DiferenciaParticipeDistribucionBanda> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<DiferenciaParticipeDistribucionBanda> detalle) {
        this.detalle = detalle;
    }
}
