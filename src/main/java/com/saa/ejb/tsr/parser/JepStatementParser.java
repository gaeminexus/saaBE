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

import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.rubros.ASPEstadoRevisionExtracto;

/**
 * @author GaemiSoft
 * <p>Parser de estados de cuenta de Coop. JEP. Verificado contra archivo
 * real "COOP. JEP.xlsx": encabezado en fila 2 (1-indexado) = fila 1
 * (0-indexado): Fecha | Descripcion/Referencia | Oficina Transaccion |
 * Debito(s) | Credito(s) | Saldos. Fecha nativa de Excel, Debito/Credito
 * columnas separadas NUMERIC.</p>
 * <p>Confirmado: JEP efectivamente mezcla filas "Saldo inicial" y
 * "Saldo Final" dentro de la misma tabla de movimientos (ej. la fila
 * "Saldo Final" trae Credito(s) duplicado de la ultima transaccion real y un
 * Saldo de 0.00 sin sentido) - ambas se descartan como transaccion. El valor
 * de la fila "Saldo inicial" SI se conserva como saldo inicial real del
 * periodo (mas confiable que derivarlo).</p>
 * <p><b>Nota de instanciacion:</b> esta clase guarda el saldo inicial
 * capturado en un campo de instancia mientras procesa las filas - por eso
 * {@link BankStatementParserFactory} debe crear una instancia nueva por cada
 * llamado a parse(), nunca reusar/compartir una instancia entre hilos.</p>
 */
public class JepStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA = 0;
    private static final int COL_DESCRIPCION_REFERENCIA = 1;
    private static final int COL_DEBITO = 3;
    private static final int COL_CREDITO = 4;
    private static final int COL_SALDOS = 5;

    private Double saldoInicialCapturado;

    @Override
    protected int getPrimeraFilaDatos() {
        return 2;
    }

    @Override
    protected boolean encabezadoValido(Row filaEncabezado) {
        return columnaContiene(filaEncabezado, COL_FECHA, "fecha")
                && columnaContiene(filaEncabezado, COL_DEBITO, "bito")
                && columnaContiene(filaEncabezado, COL_SALDOS, "saldo");
    }

    @Override
    protected Double getSaldoInicialCapturado() {
        return saldoInicialCapturado;
    }

    @Override
    protected DetalleExtractoBancario parseRow(Row row) throws Throwable {
        Cell celdaFecha = row.getCell(COL_FECHA);
        // POI puede devolver una Cell no nula pero de tipo BLANK para celdas con
        // formato aplicado mas alla del rango real de datos (confirmado: el
        // archivo real de JEP trae 6 filas asi despues de la ultima transaccion,
        // que sin este chequeo se leian como transacciones con fecha 1899-12-31).
        if (celdaFecha == null || celdaFecha.getCellType() == CellType.BLANK) {
            return null;
        }

        String descripcion = getCellString(row, COL_DESCRIPCION_REFERENCIA);
        String descripcionNormalizada = descripcion.trim().toLowerCase();

        if (descripcionNormalizada.equals("saldo inicial")) {
            saldoInicialCapturado = getCellMontoUS(row, COL_SALDOS);
            return null;
        }
        if (descripcionNormalizada.equals("saldo final")) {
            // El "Saldo" de esta fila no es confiable (ver comentario de clase) -
            // se descarta sin capturar nada; el saldo final real queda dado por
            // la ultima transaccion real ya procesada.
            return null;
        }

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(getCellFechaNativa(row, COL_FECHA));
        d.setDescripcion(descripcion);
        d.setDebito(getCellMontoUS(row, COL_DEBITO));
        d.setCredito(getCellMontoUS(row, COL_CREDITO));
        d.setSaldo(getCellMontoUS(row, COL_SALDOS));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
