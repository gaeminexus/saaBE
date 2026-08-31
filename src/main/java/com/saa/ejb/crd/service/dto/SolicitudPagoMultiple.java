package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Body de POST /rest/prst/pagarMultiplesCuotas.
 *
 * Cobro de VARIOS préstamos del MISMO partícipe en una sola operación (una sola confirmación,
 * un solo comprobante). Cada renglón es exactamente lo mismo que hoy acepta
 * {@code /pagarCuota} — no es un formato nuevo, es una lista del mismo objeto.
 */
public class SolicitudPagoMultiple {

    private List<SolicitudPagoCuota> pagos = new ArrayList<>();

    /**
     * Empresa contable (SCP.PJRQ) sobre la que se genera el asiento. Obligatorio.
     *
     * Lo manda el frontend desde la empresa de la sesión. Cuando la llamada viene de
     * CobroCreditoServiceImpl.procesarCobro/anularCobro, lo pone ese servicio con la empresa
     * derivada de la cuenta bancaria del cobro, NO con la que vino del cliente.
     */
    private Long idEmpresa;

    public SolicitudPagoMultiple() {
    }

    public List<SolicitudPagoCuota> getPagos() {
        return pagos;
    }

    public void setPagos(List<SolicitudPagoCuota> pagos) {
        this.pagos = pagos;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }
}
