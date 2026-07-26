/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.parser;

import org.apache.poi.ss.usermodel.Row;

import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.rubros.ASPEstadoRevisionExtracto;

/**
 * @author GaemiSoft
 * <p>Parser de estados de cuenta de Coop. Alianza. Verificado contra archivo
 * real "COOP. ALIANZA 2026.xls" (BIFF legado, HSSF): encabezado en fila 7
 * (1-indexado) = fila 6 (0-indexado): FECHA | CONCEPTO | TIPO | VALOR
 * EFECTIVO | VALOR CHEQUE | SALDO. Fechas en texto <b>mm/dd/yyyy</b>
 * (hardcodeado, nunca inferido - ambiguo con dd/mm/yyyy).</p>
 * <p><b>Correccion sobre la propuesta original:</b> no hay columnas
 * separadas de Debito/Credito - el monto es VALOR EFECTIVO + VALOR CHEQUE, y
 * el signo lo da el codigo "TIPO" ("NC" = credito, confirmado en la
 * muestra). La muestra real solo trajo movimientos de credito (2 filas); se
 * asume "ND" para debito por la misma convencion vista en Internacional/
 * Pacifico/Mutualista Pichincha ("Nota de Debito"/"Nota de Credito") - sin
 * confirmar contra una fila real de debito. Recomendado pedir al cliente una
 * muestra mas larga antes de dar esto por cerrado. No trae saldo inicial
 * declarado - se deriva de la primera fila.</p>
 */
public class AlianzaStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA = 0;
    private static final int COL_CONCEPTO = 1;
    private static final int COL_TIPO = 2;
    private static final int COL_VALOR_EFECTIVO = 3;
    private static final int COL_VALOR_CHEQUE = 4;
    private static final int COL_SALDO = 5;

    @Override
    protected int getPrimeraFilaDatos() {
        return 7;
    }

    @Override
    protected DetalleExtractoBancario parseRow(Row row) throws Throwable {
        String fechaTexto = getCellString(row, COL_FECHA);
        if (fechaTexto.isBlank()) {
            return null;
        }

        String tipo = getCellString(row, COL_TIPO).trim();
        boolean esDebito = tipo.equalsIgnoreCase("ND");
        double valorEfectivo = getCellMontoUS(row, COL_VALOR_EFECTIVO) != null
                ? getCellMontoUS(row, COL_VALOR_EFECTIVO) : 0.0;
        double valorCheque = getCellMontoUS(row, COL_VALOR_CHEQUE) != null
                ? getCellMontoUS(row, COL_VALOR_CHEQUE) : 0.0;
        double monto = valorEfectivo + valorCheque;

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(parseFechaMDY(fechaTexto));
        d.setCodigoMovimiento(tipo);
        d.setDescripcion(getCellString(row, COL_CONCEPTO));
        d.setDebito(esDebito ? monto : null);
        d.setCredito(esDebito ? null : monto);
        d.setSaldo(getCellMontoUS(row, COL_SALDO));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
