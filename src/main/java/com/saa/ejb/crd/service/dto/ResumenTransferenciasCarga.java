package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de {@code GET /rest/asgn/transferencias/{idCarga}} — CONTRATO CONGELADO, ver
 * {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md} §2.1.
 */
public class ResumenTransferenciasCarga {

    private Long idCarga;
    /** "yyyy-MM" (año/mes de afectación de la carga). */
    private String periodo;
    private String nombreFilial;
    /** Lo que el archivo dice que se descontó (CargaArchivo.totalDescontado). */
    private Double totalArchivo;
    /** Suma de las transferencias VIGENTES. */
    private Double totalTransferencias;
    /** totalArchivo − totalTransferencias. */
    private Double diferencia;
    /** |diferencia| <= 0.01. */
    private Boolean cuadra;
    /** ¿Ya se hizo el paso 1? Se deriva de fechaAutorizacionContabilidad != null, NUNCA de CRARESTD. */
    private Boolean confirmada;
    private String usuarioConfirma;
    private LocalDateTime fechaConfirmacion;
    private List<TransferenciaCargaPetroDTO> transferencias = new ArrayList<>();

    public Long getIdCarga() {
        return idCarga;
    }

    public void setIdCarga(Long idCarga) {
        this.idCarga = idCarga;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public String getNombreFilial() {
        return nombreFilial;
    }

    public void setNombreFilial(String nombreFilial) {
        this.nombreFilial = nombreFilial;
    }

    public Double getTotalArchivo() {
        return totalArchivo;
    }

    public void setTotalArchivo(Double totalArchivo) {
        this.totalArchivo = totalArchivo;
    }

    public Double getTotalTransferencias() {
        return totalTransferencias;
    }

    public void setTotalTransferencias(Double totalTransferencias) {
        this.totalTransferencias = totalTransferencias;
    }

    public Double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(Double diferencia) {
        this.diferencia = diferencia;
    }

    public Boolean getCuadra() {
        return cuadra;
    }

    public void setCuadra(Boolean cuadra) {
        this.cuadra = cuadra;
    }

    public Boolean getConfirmada() {
        return confirmada;
    }

    public void setConfirmada(Boolean confirmada) {
        this.confirmada = confirmada;
    }

    public String getUsuarioConfirma() {
        return usuarioConfirma;
    }

    public void setUsuarioConfirma(String usuarioConfirma) {
        this.usuarioConfirma = usuarioConfirma;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) {
        this.fechaConfirmacion = fechaConfirmacion;
    }

    public List<TransferenciaCargaPetroDTO> getTransferencias() {
        return transferencias;
    }

    public void setTransferencias(List<TransferenciaCargaPetroDTO> transferencias) {
        this.transferencias = transferencias;
    }
}
