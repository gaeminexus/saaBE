/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.parser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.rubros.ASPEstadoRevisionExtracto;

/**
 * @author GaemiSoft
 * <p>Parser de estados de cuenta de Banco Atlantida. Verificado contra
 * archivo real "B. ANTLANTIDA.xlsx": fila 0 trae "Saldo Inicial:" con el
 * valor en la celda siguiente, fila 1 vacia, encabezado en fila 2
 * (0-indexado) = fila 3 (1-indexado): FechaMov | Sucursal | Asiento |
 * Transaccion | Concepto | Debito | Credito | Cheques | Saldos Disponibles |
 * Contable. Fecha nativa de Excel, Debito/Credito columnas separadas
 * NUMERIC.</p>
 */
public class AtlantidaStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA = 0;
    private static final int COL_ASIENTO = 2;
    private static final int COL_TRANSACCION = 3;
    private static final int COL_CONCEPTO = 4;
    private static final int COL_DEBITO = 5;
    private static final int COL_CREDITO = 6;
    private static final int COL_SALDOS_DISPONIBLES = 8;

    @Override
    protected int getPrimeraFilaDatos() {
        return 3;
    }

    @Override
    protected Double getSaldoInicialDeclarado(Sheet sheet) {
        Row fila0 = sheet.getRow(0);
        if (fila0 == null) {
            return null;
        }
        Cell celda = fila0.getCell(1);
        return celda == null ? null : celda.getNumericCellValue();
    }

    @Override
    protected DetalleExtractoBancario parseRow(Row row) throws Throwable {
        Cell celdaFecha = row.getCell(COL_FECHA);
        // POI puede devolver una Cell no nula pero de tipo BLANK para celdas con
        // formato aplicado mas alla del rango real de datos (bug confirmado con
        // este mismo patron en JepStatementParser) - se chequea explicitamente.
        if (celdaFecha == null || celdaFecha.getCellType() == CellType.BLANK) {
            return null;
        }

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(getCellFechaNativa(row, COL_FECHA));
        d.setReferencia(getCellString(row, COL_ASIENTO));
        d.setCodigoMovimiento(getCellString(row, COL_TRANSACCION));
        d.setDescripcion(getCellString(row, COL_CONCEPTO));
        d.setDebito(getCellMontoUS(row, COL_DEBITO));
        d.setCredito(getCellMontoUS(row, COL_CREDITO));
        d.setSaldo(getCellMontoUS(row, COL_SALDOS_DISPONIBLES));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
