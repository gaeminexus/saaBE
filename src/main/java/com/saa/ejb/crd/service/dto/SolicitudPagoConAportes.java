package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Body de POST /rest/prst/pagarConAportes.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §7.4.
 */
public class SolicitudPagoConAportes {

    private Long idPrestamo;

    /** Desglose por tipo de aporte; el valor total del pago es la suma de sus renglones */
    private List<DesgloseAporte> aportes = new ArrayList<>();

    private String usuario;
    private String observacion;

    /** Fecha de negocio del pago; si es null se asume hoy */
    private LocalDate fechaPago;

    /** Ruta del documento de respaldo digitalizado; se estampa en los pagos generados */
    private String rutaDocumentoRespaldo;

    /**
     * Empresa contable (SCP.PJRQ) sobre la que se genera el asiento. Obligatorio.
     *
     * Lo manda el frontend desde la empresa de la sesión. Cuando la llamada viene de
     * CobroCreditoServiceImpl.procesarCobro/anularCobro, lo pone ese servicio con la empresa
     * derivada de la cuenta bancaria del cobro, NO con la que vino del cliente.
     */
    private Long idEmpresa;

    public SolicitudPagoConAportes() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
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

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getRutaDocumentoRespaldo() {
        return rutaDocumentoRespaldo;
    }

    public void setRutaDocumentoRespaldo(String rutaDocumentoRespaldo) {
        this.rutaDocumentoRespaldo = rutaDocumentoRespaldo;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }
}
