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
 * <p>Parser de estados de cuenta de Banco Guayaquil. Verificado contra
 * archivo real "B. GUAYAQUIL 2026.xlsx": encabezado en fila 14 (1-indexado)
 * = fila 13 (0-indexado): # | Fecha de transaccion | Fecha contable | Tipo
 * de movimiento | Documento | Concepto | Agencia | Monto | Saldo efectivo |
 * Saldo total | Referencia | Referencia 2 | Referencia 3 | Signo. Fechas en
 * texto ISO (no datetime nativo). Monto y saldo en texto con convencion US
 * ("$3,617,432.99") mas columna "Signo" (+/-) separada. No trae saldo
 * inicial declarado - se deriva de la primera fila.</p>
 */
public class GuayaquilStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA_TRANSACCION = 1;
    private static final int COL_TIPO_MOVIMIENTO = 3;
    private static final int COL_DOCUMENTO = 4;
    private static final int COL_CONCEPTO = 5;
    private static final int COL_MONTO = 7;
    private static final int COL_SALDO_TOTAL = 9;
    private static final int COL_SIGNO = 13;

    @Override
    protected int getPrimeraFilaDatos() {
        return 14;
    }

    @Override
    protected boolean encabezadoValido(Row filaEncabezado) {
        // Guayaquil y Manabi comparten vocabulario de encabezado
        // ("Monto"/"Saldo Total"/"Concepto") en la misma fila 14 - solo la
        // posicion exacta de columna distingue uno del otro (ver javadoc de
        // AbstractExcelStatementParser.encabezadoValido()).
        return columnaContiene(filaEncabezado, COL_FECHA_TRANSACCION, "fecha")
                && columnaContiene(filaEncabezado, COL_MONTO, "monto")
                && columnaContiene(filaEncabezado, COL_SALDO_TOTAL, "saldo");
    }

    @Override
    protected DetalleExtractoBancario parseRow(Row row) throws Throwable {
        String fechaTexto = getCellString(row, COL_FECHA_TRANSACCION);
        if (fechaTexto.isBlank()) {
            return null;
        }

        String signo = getCellString(row, COL_SIGNO).trim();
        Double monto = getCellMontoUS(row, COL_MONTO);
        boolean esDebito = "-".equals(signo);

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(parseFechaISO(fechaTexto));
        d.setCodigoMovimiento(getCellString(row, COL_TIPO_MOVIMIENTO));
        d.setDescripcion(getCellString(row, COL_CONCEPTO));
        d.setReferencia(getCellString(row, COL_DOCUMENTO));
        d.setDebito(esDebito ? monto : null);
        d.setCredito(esDebito ? null : monto);
        d.setSaldo(getCellMontoUS(row, COL_SALDO_TOTAL));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
