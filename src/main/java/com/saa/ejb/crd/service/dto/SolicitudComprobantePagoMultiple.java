package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Body de POST /rest/prst/comprobantePagoMultiple.
 *
 * Solo identificadores — el backend reconstruye TODO lo demás (montos, socio, producto) desde
 * CRD.EVPR/CRD.PGPR. No recibe montos del cliente: un comprobante financiero no se imprime con
 * datos que el cliente pueda alterar antes de pedirlo.
 */
public class SolicitudComprobantePagoMultiple {

    /** Códigos de EventoPrestamo devueltos por pagarMultiplesCuotas (uno por préstamo pagado) */
    private List<Long> idsEvento = new ArrayList<>();

    public SolicitudComprobantePagoMultiple() {
    }

    public List<Long> getIdsEvento() {
        return idsEvento;
    }

    public void setIdsEvento(List<Long> idsEvento) {
        this.idsEvento = idsEvento;
    }
}
