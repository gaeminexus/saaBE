/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.parser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.rubros.ASPEstadoRevisionExtracto;

/**
 * @author GaemiSoft
 * <p>Parser de estados de cuenta de Banco Austro. Verificado contra archivo
 * real "B. AUSTRO 2026.XLSX".</p>
 * <p><b>Correccion sobre la propuesta original:</b> el encabezado real esta
 * en la fila 10 (1-indexado) = fila 9 (0-indexado), no en la fila 8 -
 * FECHA REAL | FECHA CONTABLE | DESCRIPCION | SUCURSAL | OFICINA | USUARIO |
 * REFERENCIA | Num.PAPELETA | DEBITOS | CREDITOS | SALDO CONTABLE. Las
 * fechas llegan en texto ISO con hora ("2026-06-30 22:44:55.81"), no como
 * datetime nativo. El saldo inicial ("Saldo Ini:") esta en la fila 8
 * (0-indexado), columna 8.</p>
 */
public class AustroStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA_REAL = 1;
    private static final int COL_DESCRIPCION = 3;
    private static final int COL_REFERENCIA = 7;
    private static final int COL_DEBITOS = 9;
    private static final int COL_CREDITOS = 10;
    private static final int COL_SALDO_CONTABLE = 11;

    @Override
    protected int getPrimeraFilaDatos() {
        return 10;
    }

    @Override
    protected boolean encabezadoValido(Row filaEncabezado) {
        return columnaContiene(filaEncabezado, COL_FECHA_REAL, "fecha")
                && columnaContiene(filaEncabezado, COL_DEBITOS, "bito")
                && columnaContiene(filaEncabezado, COL_SALDO_CONTABLE, "saldo");
    }

    @Override
    protected Double getSaldoInicialDeclarado(Sheet sheet) {
        Row fila8 = sheet.getRow(8);
        if (fila8 == null) {
            return null;
        }
        Cell celda = fila8.getCell(8);
        return celda == null ? null : celda.getNumericCellValue();
    }

    @Override
    protected DetalleExtractoBancario parseRow(Row row) throws Throwable {
        String fechaTexto = getCellString(row, COL_FECHA_REAL);
        if (fechaTexto.isBlank()) {
            return null;
        }

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(parseFechaISO(fechaTexto));
        d.setDescripcion(getCellString(row, COL_DESCRIPCION));
        d.setReferencia(getCellString(row, COL_REFERENCIA));
        d.setDebito(getCellMontoUS(row, COL_DEBITOS));
        d.setCredito(getCellMontoUS(row, COL_CREDITOS));
        d.setSaldo(getCellMontoUS(row, COL_SALDO_CONTABLE));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
