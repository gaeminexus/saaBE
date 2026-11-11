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

    /**
     * Código del {@code CobroCredito} (CRD.CBCR) que originó esta llamada — mismo
     * discriminador que {@link SolicitudPrecancelacion#getIdCobroCredito()}, agregado acá por
     * consistencia (2026-08-31) aunque hoy {@code CobroCreditoServiceImpl} no llame a
     * {@code pagarConAportes} internamente (0 referencias, verificado 2026-08-31) — este
     * endpoint es siempre una llamada directa, así que {@code contabilizarPagoConAportes}
     * sigue generando su asiento sin condicionar por este campo. Si algún día CBCR empieza a
     * llamarlo, el gate se agrega ahí, no se infiere de la ausencia del campo.
     *
     * <b>Nunca lo manda el cliente.</b>
     */
    private Long idCobroCredito;

    /**
     * Opcional: PK de CRD.GRPR del garante activo del préstamo. Con él, los aportes que se
     * validan y consumen son los del PARTÍCIPE GARANTE y no los del deudor (el préstamo y sus
     * cuotas siguen siendo del deudor); exige {@code rutaDocumentoRespaldo} (la autorización
     * firmada del garante). Sin él, todo igual que siempre. Ver
     * DISENO-PAGO-CON-APORTES-DEL-GARANTE.md §G2.
     */
    private Long idGarante;

    public SolicitudPagoConAportes() {
    }

    public Long getIdGarante() {
        return idGarante;
    }

    public void setIdGarante(Long idGarante) {
        this.idGarante = idGarante;
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

    public Long getIdCobroCredito() {
        return idCobroCredito;
    }

    public void setIdCobroCredito(Long idCobroCredito) {
        this.idCobroCredito = idCobroCredito;
    }
}
