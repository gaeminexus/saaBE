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
 * <p>Parser de estados de cuenta de Coop. Policia Nacional. Verificado
 * contra archivo real "COO POLIC[I]A NACIONAL.xlsx": fila 1 (0-indexado)
 * trae "Saldo Anterior" en columna 0 con el valor en columna 2, encabezado
 * en fila 4 (0-indexado) = fila 5 (1-indexado): Fecha | Lugar | Causa |
 * Descripcion | +/- | Efectivo | Cheques | Valor | Intereses | Saldo
 * Contable | Saldo Disponibles.</p>
 * <p>Fecha nativa de Excel. A diferencia de Guayaquil/Manabi, "Valor" llega
 * como celda NUMERIC nativa (no como texto con "$"), pese a mostrarse
 * formateada con signo de moneda - no hace falta parsear texto.</p>
 */
public class PoliciaNacionalStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA = 0;
    private static final int COL_CAUSA = 2;
    private static final int COL_DESCRIPCION = 3;
    private static final int COL_SIGNO = 4;
    private static final int COL_VALOR = 7;
    private static final int COL_SALDO_CONTABLE = 9;

    @Override
    protected int getPrimeraFilaDatos() {
        return 5;
    }

    @Override
    protected Double getSaldoInicialDeclarado(Sheet sheet) {
        Row fila1 = sheet.getRow(1);
        if (fila1 == null) {
            return null;
        }
        Cell celda = fila1.getCell(2);
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

        String signo = getCellString(row, COL_SIGNO).trim();
        Double valor = getCellMontoUS(row, COL_VALOR);
        boolean esDebito = "-".equals(signo);

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(getCellFechaNativa(row, COL_FECHA));
        d.setCodigoMovimiento(getCellString(row, COL_CAUSA));
        d.setDescripcion(getCellString(row, COL_DESCRIPCION));
        d.setDebito(esDebito ? valor : null);
        d.setCredito(esDebito ? null : valor);
        d.setSaldo(getCellMontoUS(row, COL_SALDO_CONTABLE));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
