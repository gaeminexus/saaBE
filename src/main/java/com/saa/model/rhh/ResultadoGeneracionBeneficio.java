package com.saa.model.rhh;

import java.io.Serializable;

/**
 * Resultado de generar el decimo tercero o el decimo cuarto (POST lqbs/generarDecimoTercero y
 * lqbs/generarDecimoCuarto): cuantas liquidaciones se generaron o actualizaron y cuantas se
 * dejaron como estaban por estar ya PAGADAS o dentro de una orden (A0 del diseno
 * DISENO-DECIMO-TERCERO-ANTICIPADO.md).
 */
public class ResultadoGeneracionBeneficio implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Liquidaciones generadas o recalculadas. */
    private int generados;

    /** Liquidaciones que no se recalcularon por estar PAGADAS o en una orden. */
    private int omitidosPagados;

    public ResultadoGeneracionBeneficio() {
    }

    public ResultadoGeneracionBeneficio(int generados, int omitidosPagados) {
        this.generados = generados;
        this.omitidosPagados = omitidosPagados;
    }

    public int getGenerados() {
        return generados;
    }

    public void setGenerados(int generados) {
        this.generados = generados;
    }

    public int getOmitidosPagados() {
        return omitidosPagados;
    }

    public void setOmitidosPagados(int omitidosPagados) {
        this.omitidosPagados = omitidosPagados;
    }
}
