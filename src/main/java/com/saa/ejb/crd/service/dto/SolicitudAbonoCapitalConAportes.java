package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Body de POST /rest/prst/abonarCapitalConAportes — abono a capital 100% con aportes del
 * partícipe, aplicado en el acto (API-ABONO-CAPITAL-CON-APORTES.md §2).
 *
 * No hay campo {@code valor}: el abono es {@code Σ aportes}.
 */
public class SolicitudAbonoCapitalConAportes {

    private Long idEmpresa;
    private Long idPrestamo;
    /** 1 = mantiene la cuota y reduce el plazo; 2 = mantiene el plazo y reduce la cuota */
    private Integer modalidad;
    private List<DesgloseAporte> aportes = new ArrayList<>();
    private String usuario;
    private String observacion;
    /** Fecha de pago real; si es null se asume hoy */
    private LocalDate fecha;
    /** Fecha contable del asiento. Obligatoria (mismas reglas que la precancelación directa). */
    private LocalDate fechaAfectacion;
    /** Opcional: no entra dinero de afuera, no hay comprobante bancario. */
    private String rutaDocumentoRespaldo;

    /** C16b: {@code false} = registrar el abono sobre la tabla vigente sin re-amortizar (ver
     * {@link SolicitudAbonoCapital#getRegenerarTabla()}); {@code null}/{@code true} = de siempre. */
    private Boolean regenerarTabla;

    /** C16b: cuota donde se registra el abono sin regenerar tabla; {@code null} = la vigente. */
    private Integer numeroCuota;

    /** D-048: {@code true} = re-proceso; "al día" se evalúa a la fecha del abono (ver
     * {@link SolicitudAbonoCapital#getReproceso()}). */
    private Boolean reproceso;

    public SolicitudAbonoCapitalConAportes() {
    }

    public Boolean getReproceso() {
        return reproceso;
    }

    public void setReproceso(Boolean reproceso) {
        this.reproceso = reproceso;
    }

    public Boolean getRegenerarTabla() {
        return regenerarTabla;
    }

    public void setRegenerarTabla(Boolean regenerarTabla) {
        this.regenerarTabla = regenerarTabla;
    }

    public Integer getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Integer numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Integer getModalidad() {
        return modalidad;
    }

    public void setModalidad(Integer modalidad) {
        this.modalidad = modalidad;
    }

    public List<DesgloseAporte> getAportes() {
        return aportes;
    }

    public void setAportes(List<DesgloseAporte> aportes) {
        this.aportes = aportes;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalDate getFechaAfectacion() {
        return fechaAfectacion;
    }

    public void setFechaAfectacion(LocalDate fechaAfectacion) {
        this.fechaAfectacion = fechaAfectacion;
    }

    public String getRutaDocumentoRespaldo() {
        return rutaDocumentoRespaldo;
    }

    public void setRutaDocumentoRespaldo(String rutaDocumentoRespaldo) {
        this.rutaDocumentoRespaldo = rutaDocumentoRespaldo;
    }
}
