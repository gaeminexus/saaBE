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
 * <p>Parser de estados de cuenta de Banco Manabi (cuenta de Ahorro).
 * Verificado contra archivo real "B. MANABI AHO 2026.xlsx": encabezado en
 * fila 14 (1-indexado) = fila 13 (0-indexado): Fecha - Hora | Concepto |
 * Numero de Movimiento | Tipo Movimiento | Canal | Monto | Saldo Total.</p>
 * <p><b>Correccion sobre la propuesta original:</b> no hay columna de signo
 * (+/-) - el debito/credito se determina por la palabra completa en "Tipo
 * Movimiento" ("CREDITO"/"DEBITO"), no por un simbolo. Fechas en texto ISO
 * con hora (no datetime nativo). Monto y saldo en texto con convencion
 * europea/latina ("$ 2.517,34" - punto=miles, coma=decimal), convencion
 * OPUESTA a la de Guayaquil. No trae saldo inicial declarado - se deriva de
 * la primera fila.</p>
 * <p><b>Confirmado probando contra el archivo real:</b> las filas vienen en
 * orden cronologico DESCENDENTE (mas reciente primero), igual que Mutualista
 * Pichincha - la clase base ({@link AbstractExcelStatementParser}) ordena
 * siempre por fecha ascendente automaticamente, asi que esto no requiere
 * ninguna declaracion especial de esta subclase.</p>
 */
public class ManabiStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA_HORA = 0;
    private static final int COL_CONCEPTO = 1;
    private static final int COL_NUMERO_MOVIMIENTO = 2;
    private static final int COL_TIPO_MOVIMIENTO = 3;
    private static final int COL_MONTO = 5;
    private static final int COL_SALDO_TOTAL = 6;

    @Override
    protected int getPrimeraFilaDatos() {
        return 14;
    }

    @Override
    protected boolean encabezadoValido(Row filaEncabezado) {
        // Manabi y Guayaquil comparten vocabulario de encabezado
        // ("Monto"/"Saldo Total"/"Concepto") en la misma fila 14 - solo la
        // posicion exacta de columna distingue uno del otro (ver javadoc de
        // AbstractExcelStatementParser.encabezadoValido()).
        return columnaContiene(filaEncabezado, COL_FECHA_HORA, "fecha")
                && columnaContiene(filaEncabezado, COL_MONTO, "monto")
                && columnaContiene(filaEncabezado, COL_SALDO_TOTAL, "saldo");
    }

    @Override
    protected DetalleExtractoBancario parseRow(Row row) throws Throwable {
        String fechaTexto = getCellString(row, COL_FECHA_HORA);
        if (fechaTexto.isBlank()) {
            return null;
        }

        String tipoMovimiento = getCellString(row, COL_TIPO_MOVIMIENTO).trim();
        // El codigo llega como palabra completa ("CREDITO"/"DEBITO", con o sin
        // tilde segun la codificacion) - solo importa la primera letra.
        boolean esDebito = tipoMovimiento.toUpperCase().startsWith("D");
        Double monto = getCellMontoEuropeo(row, COL_MONTO);

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(parseFechaISO(fechaTexto));
        d.setCodigoMovimiento(tipoMovimiento);
        d.setDescripcion(getCellString(row, COL_CONCEPTO));
        d.setReferencia(getCellString(row, COL_NUMERO_MOVIMIENTO));
        d.setDebito(esDebito ? monto : null);
        d.setCredito(esDebito ? null : monto);
        d.setSaldo(getCellMontoEuropeo(row, COL_SALDO_TOTAL));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
