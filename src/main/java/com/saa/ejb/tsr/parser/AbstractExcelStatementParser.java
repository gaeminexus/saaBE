/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.DetalleExtractoBancario;

/**
 * @author GaemiSoft
 * <p>Mecanica comun a los 11 parsers de estados de cuenta bancarios:</p>
 * <ul>
 * <li>Abre el workbook detectando el formato real por firma de bytes -
 * {@link WorkbookFactory#create(InputStream)} ya hace esto internamente
 * (mira los primeros bytes del archivo), nunca decide por la extension.
 * Esto es lo que permite que el Banco Amazonas funcione aunque su archivo
 * diga ".xls" y en realidad sea un XLSX.</li>
 * <li>Recorre las filas desde la fila de datos que declara cada banco.</li>
 * <li>Ejecuta el balance replay: acumula saldoInicial + credito - debito fila
 * a fila y lo compara contra el saldo que reporta el banco en esa fila,
 * señalando cualquier discrepancia como advertencia sin abortar la carga.</li>
 * <li>Arma la fila cruda (todas las celdas separadas por " | ") para el CLOB
 * de auditoria (DEXBCRDO).</li>
 * </ul>
 * <p>Cada subclase solo declara: en que fila empiezan los datos, como mapear
 * una fila a un DetalleExtractoBancario (o null si la fila debe descartarse,
 * ej. filas de "Saldo inicial" que JEP mezcla en la tabla), si el saldo
 * inicial viene declarado en el encabezado del archivo, y si las filas vienen
 * en orden descendente (Mutualista Pichincha).</p>
 */
public abstract class AbstractExcelStatementParser implements BankStatementParser {

    protected final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public ParsedStatement parse(InputStream archivo, CuentaBancaria cuenta) throws Throwable {
        byte[] bytes = archivo.readAllBytes();
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes));
        try {
            Sheet sheet = workbook.getSheetAt(0);

            List<DetalleExtractoBancario> detalles = new ArrayList<>();
            int primeraFilaDatos = getPrimeraFilaDatos();
            int ultimaFila = sheet.getLastRowNum();
            for (int i = primeraFilaDatos; i <= ultimaFila; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                DetalleExtractoBancario detalle = parseRow(row);
                if (detalle == null) {
                    // La subclase descarto la fila (no es una transaccion real:
                    // fila vacia, "Saldo inicial"/"Saldo Final" de JEP, etc.)
                    continue;
                }
                if (detalle.getFechaTransaccion() == null || (detalle.getDebito() == null
                        && detalle.getCredito() == null && detalle.getSaldo() == null)) {
                    // Red de seguridad: una fila sin fecha o sin ningun dato
                    // financiero nunca es una transaccion real - normalmente
                    // indica una celda BLANK de POI mas alla del rango real de
                    // datos que la subclase no filtro explicitamente (ver el bug
                    // encontrado en JepStatementParser probando contra datos
                    // reales: filas fantasma con fecha 1899-12-31).
                    continue;
                }
                detalle.setCuentaBancaria(cuenta);
                detalle.setFilaCruda(construirFilaCruda(row));
                detalles.add(detalle);
            }

            if (isOrdenDescendente()) {
                Collections.reverse(detalles);
            }

            long numeroFila = 1;
            for (DetalleExtractoBancario d : detalles) {
                d.setNumeroFila(numeroFila++);
            }

            Double saldoInicial = getSaldoInicialDeclarado(sheet);
            if (saldoInicial == null) {
                saldoInicial = getSaldoInicialCapturado();
            }
            if (saldoInicial == null) {
                saldoInicial = derivarSaldoInicial(detalles);
            }

            List<String> advertencias = ejecutarBalanceReplay(saldoInicial, detalles);

            for (DetalleExtractoBancario d : detalles) {
                d.setHash(calcularHashFila(cuenta, d));
            }

            ParsedStatement resultado = new ParsedStatement();
            resultado.setSaldoInicial(saldoInicial);
            resultado.setSaldoFinal(detalles.isEmpty() ? saldoInicial
                    : detalles.get(detalles.size() - 1).getSaldo());
            if (!detalles.isEmpty()) {
                resultado.setFechaDesde(detalles.get(0).getFechaTransaccion());
                resultado.setFechaHasta(detalles.get(detalles.size() - 1).getFechaTransaccion());
            }
            resultado.setDetalles(detalles);
            resultado.setAdvertencias(advertencias);
            resultado.setFormatoDetectado(workbook instanceof HSSFWorkbook ? "XLS" : "XLSX");
            return resultado;
        } finally {
            workbook.close();
        }
    }

    /**
     * Indice (0-based) de la primera fila de datos (la fila siguiente al
     * encabezado de columnas).
     */
    protected abstract int getPrimeraFilaDatos();

    /**
     * Convierte una fila en un DetalleExtractoBancario. Debe devolver null si
     * la fila no es una transaccion real (fila vacia, fila de totales, etc.)
     * - en ese caso se descarta sin agregarse al resultado.
     * No es necesario llenar cuentaBancaria, filaCruda, numeroFila ni hash -
     * eso lo completa la clase base.
     */
    protected abstract DetalleExtractoBancario parseRow(Row row) throws Throwable;

    /**
     * Saldo inicial declarado explicitamente en el encabezado del archivo,
     * o null si el banco no lo trae (en cuyo caso se deriva de la primera
     * fila real: saldo - credito + debito).
     */
    protected Double getSaldoInicialDeclarado(Sheet sheet) throws Throwable {
        return null;
    }

    /**
     * Saldo inicial capturado por la subclase al procesar una fila especial
     * (ej. JEP trae una fila "Saldo inicial" dentro de la misma tabla de
     * movimientos, que se descarta como transaccion pero su valor se
     * conserva aqui). Se consulta despues de getSaldoInicialDeclarado() y
     * antes de recurrir a la derivacion automatica.
     */
    protected Double getSaldoInicialCapturado() {
        return null;
    }

    /**
     * true si el banco entrega las filas en orden cronologico descendente
     * (Mutualista Pichincha) - se revierten antes de numerar, calcular saldo
     * inicial derivado y correr el balance replay.
     */
    protected boolean isOrdenDescendente() {
        return false;
    }

    /**
     * Deriva el saldo inicial del periodo a partir de la primera fila real,
     * para los bancos que no declaran un saldo inicial explicito (Amazonas,
     * Manabi, Internacional, Guayaquil, Pacifico, Alianza): saldo de la
     * primera fila menos su credito mas su debito.
     */
    protected double derivarSaldoInicial(List<DetalleExtractoBancario> detalles) {
        if (detalles.isEmpty()) {
            return 0.0;
        }
        DetalleExtractoBancario primera = detalles.get(0);
        double saldo = primera.getSaldo() != null ? primera.getSaldo() : 0.0;
        double debito = primera.getDebito() != null ? primera.getDebito() : 0.0;
        double credito = primera.getCredito() != null ? primera.getCredito() : 0.0;
        return saldo - credito + debito;
    }

    /**
     * Balance replay (ver clase). Si el saldo calculado no coincide con el
     * reportado en una fila, se registra la advertencia y se re-sincroniza
     * con el saldo del banco para que la discrepancia no se arrastre a todas
     * las filas siguientes.
     */
    private List<String> ejecutarBalanceReplay(double saldoInicial, List<DetalleExtractoBancario> detalles) {
        List<String> advertencias = new ArrayList<>();
        double saldoCalculado = saldoInicial;
        for (DetalleExtractoBancario d : detalles) {
            double debito = d.getDebito() != null ? d.getDebito() : 0.0;
            double credito = d.getCredito() != null ? d.getCredito() : 0.0;
            saldoCalculado = saldoCalculado - debito + credito;
            if (d.getSaldo() != null && Math.abs(saldoCalculado - d.getSaldo()) > 0.01) {
                advertencias.add("Fila " + d.getNumeroFila() + " (" + d.getFechaTransaccion() + "): saldo calculado "
                        + String.format("%.2f", saldoCalculado) + " no coincide con saldo reportado "
                        + String.format("%.2f", d.getSaldo()));
                saldoCalculado = d.getSaldo();
            }
        }
        return advertencias;
    }

    private String calcularHashFila(CuentaBancaria cuenta, DetalleExtractoBancario d) throws Throwable {
        String base = cuenta.getCodigo() + "|" + d.getFechaTransaccion() + "|" + d.getDebito() + "|"
                + d.getCredito() + "|" + d.getDescripcion() + "|" + d.getSaldo();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(base.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    protected String construirFilaCruda(Row row) {
        StringBuilder sb = new StringBuilder();
        short ultimaCol = row.getLastCellNum();
        for (int c = 0; c < ultimaCol; c++) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(getCellString(row, c));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // HELPERS DE LECTURA DE CELDAS - a disposicion de las subclases
    // -------------------------------------------------------------------------

    protected String getCellString(Row row, int col) {
        return getCellString(row.getCell(col));
    }

    protected String getCellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    /**
     * Valor numerico de una celda que puede venir como NUMERIC nativo o como
     * texto (formateado con "$", separadores de miles, etc.) - usa
     * parseMontoTexto() como fallback generico (coma=miles, punto=decimal).
     */
    protected Double getCellMontoUS(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        String texto = getCellString(cell);
        return texto.isEmpty() ? null : parseMontoTextoUS(texto);
    }

    protected Double getCellMontoEuropeo(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        String texto = getCellString(cell);
        return texto.isEmpty() ? null : parseMontoTextoEuropeo(texto);
    }

    /**
     * Quita "$" y espacios; asume coma como separador de miles (opcional) y
     * punto como separador decimal (convencion US) - ej. "$3,617,432.99".
     * Tambien sirve, sin cambios, para montos en texto plano sin separador
     * de miles (Pacifico, Mutualista Pichincha), ya que remover una coma que
     * no esta presente es una operacion segura.
     */
    protected Double parseMontoTextoUS(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.replace("$", "").replace(" ", "").replace(",", "").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        return Double.parseDouble(limpio);
    }

    /**
     * Quita "$" y espacios; punto = separador de miles, coma = decimal
     * (convencion europea/latina) - ej. "$ 2.517,34". Convencion OPUESTA a
     * parseMontoTextoUS() - nunca usar una sobre datos de la otra.
     */
    protected Double parseMontoTextoEuropeo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.replace("$", "").replace(" ", "").replace(".", "").replace(",", ".").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        return Double.parseDouble(limpio);
    }

    /**
     * Fecha en texto ISO, con o sin hora ("2026-06-30" o
     * "2026-06-30 22:44:55.81") - toma solo los primeros 10 caracteres,
     * el campo fechaTransaccion de DEXB es LocalDate, no interesa la hora.
     */
    protected LocalDate parseFechaISO(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String soloFecha = texto.trim();
        if (soloFecha.length() > 10) {
            soloFecha = soloFecha.substring(0, 10);
        }
        return LocalDate.parse(soloFecha, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static final DateTimeFormatter FORMATO_MDY = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter FORMATO_DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Fecha en texto mm/dd/yyyy - SIEMPRE hardcodeado para el banco que lo
     * use (Coop. Alianza), nunca inferido por la forma del string, porque
     * "03/06/2026" es ambiguo con dd/mm/yyyy.
     */
    protected LocalDate parseFechaMDY(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return LocalDate.parse(texto.trim(), FORMATO_MDY);
    }

    /**
     * Fecha en texto dd/mm/yyyy (Banco Pacifico).
     */
    protected LocalDate parseFechaDMY(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return LocalDate.parse(texto.trim(), FORMATO_DMY);
    }

    /**
     * Fecha nativa de Excel (celda NUMERIC con formato de fecha aplicado).
     */
    protected LocalDate getCellFechaNativa(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        double valor = cell.getNumericCellValue();
        LocalDateTime fechaHora = DateUtil.getLocalDateTime(valor);
        return fechaHora.toLocalDate();
    }
}
