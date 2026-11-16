package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * C31c: body de {@code POST /rest/prst/sincronizarGarantia}. Sincroniza a mano la garantía en cuentas de
 * orden de un préstamo a una fecha (ver {@code GarantiaOrdenPrestamoService#sincronizarExplicito}).
 *
 * <pre>{ "idPrestamo": 1234, "fecha": "2025-06-30", "usuario": "REPROCESO",
 *   "motivo": "terminado en junio 2025" }</pre>
 */
public class SolicitudSincronizacionGarantia {

    /** Préstamo (PRSTCDGO). Obligatorio. */
    private Long idPrestamo;

    /** Fecha contable del asiento de baja (yyyy-MM-dd). Obligatoria, no futura. */
    private LocalDate fecha;

    /** Usuario que ordena la sincronización. Obligatorio, debe existir. */
    private String usuario;

    /** Empresa contable; opcional (si falta, la del asiento de entrega o la única con la plantilla 26). */
    private Long idEmpresa;

    /** Texto opcional para la glosa del asiento. */
    private String motivo;

    public SolicitudSincronizacionGarantia() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
