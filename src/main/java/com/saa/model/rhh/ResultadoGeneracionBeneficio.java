package com.saa.model.rhh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * C13 (VCN #13): filas calculadas, sin pagar y sin orden de personas que ya no corresponden (sin contrato activo en el anio). El generador
     * NO las borra: las cuenta y las informa; el retiro va como SQL autorizado con respaldo (rhh/sql/omen1-07).
     */
    private int retirables;

    /** Codigos LQBS de las filas retirables. */
    private List<Long> idsRetirables = new ArrayList<Long>();

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
    public int getRetirables() {
        return retirables;
    }

    public void setRetirables(int retirables) {
        this.retirables = retirables;
    }

    public List<Long> getIdsRetirables() {
        return idsRetirables;
    }

    public void setIdsRetirables(List<Long> idsRetirables) {
        this.idsRetirables = idsRetirables;
    }
}
