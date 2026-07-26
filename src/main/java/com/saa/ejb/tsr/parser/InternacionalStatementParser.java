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
 * <p>Parser de estados de cuenta de Banco Internacional. Verificado contra
 * archivo real "B INTERNACIONAL 2026.xls" (BIFF legado, HSSF): encabezado en
 * fila 4 (1-indexado) = fila 3 (0-indexado), fechas en texto ISO
 * (yyyy-MM-dd), Debito/Credito en columnas separadas nativas NUMERIC, Saldo
 * NUMERIC. No trae saldo inicial declarado en el encabezado (solo el rango
 * de fechas del periodo) - se deriva de la primera fila.</p>
 */
public class InternacionalStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA = 1;
    private static final int COL_CODIGO_MOVIMIENTO = 2;
    private static final int COL_DESCRIPCION = 3;
    private static final int COL_REFERENCIA = 5;
    private static final int COL_DEBITO = 7;
    private static final int COL_CREDITO = 8;
    private static final int COL_SALDO = 9;

    @Override
    protected int getPrimeraFilaDatos() {
        return 4;
    }

    @Override
    protected DetalleExtractoBancario parseRow(Row row) throws Throwable {
        String fechaTexto = getCellString(row, COL_FECHA);
        if (fechaTexto.isBlank()) {
            return null;
        }

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(parseFechaISO(fechaTexto));
        d.setCodigoMovimiento(getCellString(row, COL_CODIGO_MOVIMIENTO));
        d.setDescripcion(getCellString(row, COL_DESCRIPCION));
        d.setReferencia(getCellString(row, COL_REFERENCIA));
        d.setDebito(getCellMontoUS(row, COL_DEBITO));
        d.setCredito(getCellMontoUS(row, COL_CREDITO));
        d.setSaldo(getCellMontoUS(row, COL_SALDO));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
