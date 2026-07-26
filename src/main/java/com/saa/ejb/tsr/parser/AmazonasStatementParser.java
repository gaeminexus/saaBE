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
 * <p>Parser de estados de cuenta de Banco Amazonas. El archivo real
 * "B. AMAZONAS 2026.xls" trae la extension ".xls" pero su contenido real es
 * un XLSX (ZIP) valido - se verifico extrayendolo y comprobando el checksum
 * de cada parte interna, sin errores; no esta corrupto, solo trae la
 * extension equivocada. La deteccion de formato por firma de bytes ya la
 * hace {@link AbstractExcelStatementParser} (via WorkbookFactory), asi que
 * este parser no necesita saber nada especial al respecto.</p>
 * <p>Verificado contra el archivo real: encabezado en fila 7 (1-indexado) =
 * fila 6 (0-indexado): Id | Fecha | Descripcion | Debito | Credito |
 * Balance. Fechas en texto dd/mm/yyyy. Debito/Credito/Balance llegan como
 * texto con convencion US (coma=miles, punto=decimal), igual que Guayaquil -
 * no como numeros nativos. No trae saldo inicial declarado (solo el balance
 * final de corte) - se deriva de la primera fila; se verifico manualmente
 * que esa derivacion cuadra en las 3 filas de la muestra real.</p>
 */
public class AmazonasStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA = 1;
    private static final int COL_DESCRIPCION = 2;
    private static final int COL_DEBITO = 3;
    private static final int COL_CREDITO = 4;
    private static final int COL_BALANCE = 5;

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

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(parseFechaDMY(fechaTexto));
        d.setDescripcion(getCellString(row, COL_DESCRIPCION));
        d.setDebito(getCellMontoUS(row, COL_DEBITO));
        d.setCredito(getCellMontoUS(row, COL_CREDITO));
        d.setSaldo(getCellMontoUS(row, COL_BALANCE));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
