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
 * <p>Parser de estados de cuenta de Mutualista Pichincha. Verificado contra
 * archivo real "MUT. PICHINCHA 2026.xls" (BIFF legado, HSSF).</p>
 * <p><b>Correccion sobre la propuesta original:</b> el encabezado real esta
 * en la fila 18 (1-indexado) = fila 17 (0-indexado), no en la fila 19 - esa
 * fila 19 es en realidad un sub-titulo ("AHORRO CORRIENTE") que antecede a
 * los datos, con muchas columnas vacias intermedias por celdas combinadas:
 * columna 1=Fecha, 3=Concepto, 8=Debito, 11=Credito, 16=Saldo. Debito/
 * Credito llegan como texto plano (no NUMERIC), sin separador de miles en la
 * muestra vista. Confirmado: las filas vienen en orden cronologico
 * DESCENDENTE (mas reciente primero) - la clase base
 * ({@link AbstractExcelStatementParser}) ordena siempre por fecha ascendente
 * automaticamente, asi que esto no requiere ninguna declaracion especial de
 * esta subclase.</p>
 */
public class PichinchaStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA = 1;
    private static final int COL_CONCEPTO = 3;
    private static final int COL_DEBITO = 8;
    private static final int COL_CREDITO = 11;
    private static final int COL_SALDO = 16;

    @Override
    protected int getPrimeraFilaDatos() {
        return 18;
    }

    @Override
    protected boolean encabezadoValido(Row filaEncabezado) {
        return columnaContiene(filaEncabezado, COL_FECHA, "fecha")
                && columnaContiene(filaEncabezado, COL_DEBITO, "bito")
                && columnaContiene(filaEncabezado, COL_SALDO, "saldo");
    }

    @Override
    protected DetalleExtractoBancario parseRow(Row row) throws Throwable {
        // El sub-titulo de producto ("AHORRO CORRIENTE") y filas realmente
        // vacias no traen saldo - ese es el filtro mas confiable para
        // distinguir una transaccion real en este layout con tantas columnas
        // combinadas/vacias.
        String saldoTexto = getCellString(row, COL_SALDO);
        if (saldoTexto.isBlank()) {
            return null;
        }

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(parseFechaISO(getCellString(row, COL_FECHA)));
        d.setDescripcion(getCellString(row, COL_CONCEPTO));
        d.setDebito(getCellMontoUS(row, COL_DEBITO));
        d.setCredito(getCellMontoUS(row, COL_CREDITO));
        d.setSaldo(parseMontoTextoUS(saldoTexto));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
