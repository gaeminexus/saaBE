package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de {@code AbonoCapitalPrestamoService#abonarConAportes}: el mismo
 * {@link ResultadoAbonoCapital} que devuelve el abono por CBCR, más los movimientos de aporte
 * generados (misma forma que en {@code POST /prst/pagarConAportes}).
 */
public class ResultadoAbonoCapitalConAportes {

    private ResultadoAbonoCapital resultado;
    private List<MovimientoAporte> movimientosAporte = new ArrayList<>();

    public ResultadoAbonoCapitalConAportes() {
    }

    public ResultadoAbonoCapital getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoAbonoCapital resultado) {
        this.resultado = resultado;
    }

    public List<MovimientoAporte> getMovimientosAporte() {
        return movimientosAporte;
    }

    public void setMovimientosAporte(List<MovimientoAporte> movimientosAporte) {
        this.movimientosAporte = movimientosAporte;
    }
}
