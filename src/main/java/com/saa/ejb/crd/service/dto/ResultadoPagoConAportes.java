package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de un pago de préstamo con aportes: la aplicación a cuotas más los movimientos
 * generados en CRD.APRT.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.4 y §8.
 */
public class ResultadoPagoConAportes {

    private ResultadoAplicacionPago resultado;

    private List<MovimientoAporte> movimientosAporte = new ArrayList<>();

    public ResultadoPagoConAportes() {
    }

    public ResultadoAplicacionPago getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoAplicacionPago resultado) {
        this.resultado = resultado;
    }

    public List<MovimientoAporte> getMovimientosAporte() {
        return movimientosAporte;
    }

    public void setMovimientosAporte(List<MovimientoAporte> movimientosAporte) {
        this.movimientosAporte = movimientosAporte;
    }
}
