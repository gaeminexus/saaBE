/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.parser;

import java.time.LocalDate;

import org.apache.poi.ss.usermodel.Row;

import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.rubros.ASPEstadoRevisionExtracto;

/**
 * @author GaemiSoft
 * <p>Parser de estados de cuenta de Banco Pacifico (cuentas de Ahorro y
 * Corriente comparten el mismo layout - un solo parser sirve para ambas).
 * Verificado contra los archivos reales "B PACIFICO AHORRO 2026.xlsx" y
 * "B PACIFICO CTE 2026.xlsx": encabezado en fila 1 (0-indexado), columnas
 * Estado | FechaContable | Lugar | Caja | TipoMov | Nut | Valor | Numero |
 * Concepto | SaldoDespMov | Descripcion | FechaReal | CedulaOrdenante |
 * NombreOrdenante | BancoOrdenante | ReferenciaGeneral.</p>
 * <p><b>Correccion sobre la propuesta original:</b> Pacifico NO trae columnas
 * separadas de Debito/Credito - es un monto unico ("Valor") mas un codigo de
 * tipo de movimiento ("TipoMov": "N/C" = credito, "N/D" = debito,
 * confirmado en la muestra real). No hay saldo inicial declarado en el
 * encabezado - se deriva de la primera fila.</p>
 *
 * <p><b>2026-09-08, decision explicita del usuario: la conciliacion de Pacifico va por
 * FECHA CONTABLE, no por fecha real.</b> {@code fechaTransaccion} es el campo que lee TODO
 * el motor de conciliacion -- el match por cercania de fechas
 * ({@code ConciliacionContableMatchServiceImpl}), el rango min/max de un grupo, el DTO de
 * partidas en transito ({@code ConciliacionCierreServiceImpl}) y la validacion de que el
 * movimiento caiga dentro del periodo al importar
 * ({@code ImportacionExtractoBancarioServiceImpl}) -- ninguno de esos lugares mira
 * {@code fechaContable}. Antes de este cambio, Pacifico era el UNICO de los once bancos que
 * poblaba fechaTransaccion con la fecha REAL (los otros diez ni siquiera tienen esa nocion
 * distinta), lo que dejaba a Pacifico conciliando contra un campo que el motor no usa para
 * decidir nada -- de ahi que apareciera con pendientes. No se toco el motor de conciliacion
 * para "preferir fechaContable cuando exista": habria sido un cambio de comportamiento
 * global disparado por un solo banco (afecta tambien la validacion de rango del import). El
 * lugar correcto para esta decision es el parser, que es donde vive que significa la fecha
 * de ESTE banco. La fecha real no se pierde: si difiere de la contable, se anexa a la
 * descripcion ("Fecha real: dd/MM/yyyy") -- DEXB no tiene una columna aparte para guardarla
 * ademas de {@code fechaContable} (que se sigue poblando igual que antes, sin cambios).</p>
 *
 * <p><b>Si estas por "corregir" esto porque parece un cruce de columnas: no lo es.</b> Es a
 * proposito, medido y confirmado contra el motor real el 2026-09-08.</p>
 */
public class PacificoStatementParser extends AbstractExcelStatementParser {

    private static final int COL_FECHA_CONTABLE = 1;
    private static final int COL_TIPO_MOV = 4;
    private static final int COL_VALOR = 6;
    private static final int COL_NUMERO = 7;
    private static final int COL_CONCEPTO = 8;
    private static final int COL_SALDO_DESP_MOV = 9;
    private static final int COL_DESCRIPCION = 10;
    private static final int COL_FECHA_REAL = 11;
    private static final int COL_CEDULA_ORDENANTE = 12;
    private static final int COL_NOMBRE_ORDENANTE = 13;
    private static final int COL_BANCO_ORDENANTE = 14;
    private static final int COL_REFERENCIA_GENERAL = 15;

    @Override
    protected int getPrimeraFilaDatos() {
        return 1;
    }

    @Override
    protected boolean encabezadoValido(Row filaEncabezado) {
        return columnaContiene(filaEncabezado, COL_TIPO_MOV, "tipomov")
                && columnaContiene(filaEncabezado, COL_VALOR, "valor");
    }

    @Override
    protected DetalleExtractoBancario parseRow(Row row) throws Throwable {
        String fechaContableTexto = getCellString(row, COL_FECHA_CONTABLE);
        if (fechaContableTexto.isBlank()) {
            return null;
        }

        String fechaRealTexto = getCellString(row, COL_FECHA_REAL);
        String fechaRealSoloFecha = fechaRealTexto.length() >= 10 ? fechaRealTexto.substring(0, 10) : fechaRealTexto;

        String tipoMov = getCellString(row, COL_TIPO_MOV).trim();
        Double valor = getCellMontoUS(row, COL_VALOR);
        // "N/D" (Nota de Debito) es la unica variante de debito confirmada en la
        // muestra real; cualquier otro codigo no reconocido se trata como
        // credito por defecto - el balance replay señala como advertencia
        // cualquier fila donde esta suposicion resulte incorrecta.
        boolean esDebito = tipoMov.equalsIgnoreCase("N/D");

        String referencia = getCellString(row, COL_NUMERO);
        if (referencia.isBlank()) {
            referencia = getCellString(row, COL_REFERENCIA_GENERAL);
        }

        String descripcion = getCellString(row, COL_CONCEPTO);
        String descripcionAdicional = getCellString(row, COL_DESCRIPCION);
        if (!descripcionAdicional.isBlank()) {
            descripcion = descripcion.isBlank() ? descripcionAdicional : descripcion + " - " + descripcionAdicional;
        }
        // Solo las transferencias traen datos del ordenante; se anexan a la
        // descripcion porque DEXB no tiene columnas dedicadas para ellos.
        String nombreOrdenante = getCellString(row, COL_NOMBRE_ORDENANTE);
        if (!nombreOrdenante.isBlank()) {
            descripcion += " | Ordenante: " + nombreOrdenante
                    + " (" + getCellString(row, COL_CEDULA_ORDENANTE) + ") - "
                    + getCellString(row, COL_BANCO_ORDENANTE);
        }

        LocalDate fechaContable = parseFechaDMY(fechaContableTexto);
        LocalDate fechaReal = fechaRealSoloFecha.isBlank() ? null : parseFechaDMY(fechaRealSoloFecha);
        // La conciliacion va por fecha contable (ver el javadoc de la clase) -- la fecha real
        // no se pierde, se anexa a la descripcion, y solo cuando difiere de la contable para
        // no ensuciar la mayoria de filas donde son iguales.
        if (fechaReal != null && !fechaReal.equals(fechaContable)) {
            descripcion += " | Fecha real: " + FORMATO_DMY.format(fechaReal);
        }

        DetalleExtractoBancario d = new DetalleExtractoBancario();
        d.setFechaTransaccion(fechaContable);
        d.setFechaContable(fechaContable);
        d.setCodigoMovimiento(tipoMov);
        d.setDescripcion(descripcion);
        d.setReferencia(referencia);
        d.setDebito(esDebito ? valor : null);
        d.setCredito(esDebito ? null : valor);
        d.setSaldo(getCellMontoUS(row, COL_SALDO_DESP_MOV));
        d.setEstadoRevision((long) ASPEstadoRevisionExtracto.PENDIENTE_REVISION);
        return d;
    }
}
