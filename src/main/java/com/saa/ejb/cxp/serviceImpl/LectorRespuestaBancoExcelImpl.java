package com.saa.ejb.cxp.serviceImpl;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.service.LectorRespuestaBanco;
import com.saa.ejb.cxp.service.RespuestaPagoBanco;
import com.saa.model.cxp.LotePago;

/**
 * Implementación PROVISIONAL del lector del archivo de respuesta del banco.
 *
 * ⚠ PENDIENTE: el formato oficial del archivo de respuesta todavía no fue
 * entregado. Este lector asume un Excel simple, con columnas por posición (mismo
 * criterio que la carga de tablas de amortización del módulo de crédito):
 *
 *   Fila 1        : encabezados (se omite)
 *   Columna A (0) : id del pago (PGS.PGTR.PGTRCDGO), tal como salió en el archivo enviado
 *   Columna B (1) : resultado — OK / CONFIRMADO / EJECUTADO / S / SI = confirmado;
 *                   cualquier otro valor se interpreta como rechazado
 *   Columna C (2) : número de transferencia o referencia del banco
 *   Columna D (3) : motivo del rechazo (opcional)
 *
 * Cuando llegue la especificación real basta con escribir otra implementación de
 * {@link LectorRespuestaBanco}: el resto del flujo no cambia.
 */
public class LectorRespuestaBancoExcelImpl implements LectorRespuestaBanco {

	@Override
	public List<RespuestaPagoBanco> leer(byte[] archivo, LotePago lote) throws Throwable {

		System.out.println("=== LectorRespuestaBancoExcelImpl | lote="
				+ (lote != null ? lote.getId() : null) + " ===");

		if (archivo == null || archivo.length == 0) {
			throw new IncomeException("No se recibió el archivo de respuesta del banco.");
		}

		List<RespuestaPagoBanco> respuestas = new ArrayList<>();

		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(archivo))) {
			Sheet sheet = workbook.getSheetAt(0); // Primera hoja

			// Desde la fila 2: la primera es el encabezado
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null) {
					continue;
				}

				Long idPago = leerLong(row.getCell(0));
				if (idPago == null) {
					continue; // fila vacía o sin identificador
				}

				String resultado  = leerTexto(row.getCell(1));
				String referencia = leerTexto(row.getCell(2));
				String motivo     = leerTexto(row.getCell(3));

				RespuestaPagoBanco respuesta = new RespuestaPagoBanco();
				respuesta.setIdPago(idPago);
				respuesta.setConfirmado(esConfirmado(resultado));
				respuesta.setReferencia(referencia);
				respuesta.setMotivo(motivo);
				respuestas.add(respuesta);
			}
		} catch (IncomeException e) {
			throw e;
		} catch (Exception e) {
			throw new IncomeException("No se pudo leer el archivo de respuesta del banco: "
					+ e.getMessage());
		}

		if (respuestas.isEmpty()) {
			throw new IncomeException("El archivo de respuesta no contiene ninguna fila con datos.");
		}

		System.out.println("✓ Respuestas leídas del archivo: " + respuestas.size());
		return respuestas;
	}

	// ── Helpers de lectura de celdas ─────────────────────────────────────────

	/**
	 * Interpreta el resultado informado por el banco.
	 * @param resultado : Texto de la columna de resultado
	 * @return          : true si la transferencia se ejecutó
	 */
	private boolean esConfirmado(String resultado) {
		if (resultado == null) {
			return false;
		}
		String valor = resultado.trim().toUpperCase();
		return valor.equals("OK") || valor.equals("CONFIRMADO") || valor.equals("EJECUTADO")
				|| valor.equals("S") || valor.equals("SI") || valor.equals("APROBADO")
				|| valor.equals("PROCESADO");
	}

	private Long leerLong(Cell celda) {
		if (celda == null || celda.getCellType() == CellType.BLANK) {
			return null;
		}
		try {
			if (celda.getCellType() == CellType.NUMERIC) {
				return (long) celda.getNumericCellValue();
			}
			String texto = celda.getStringCellValue().trim();
			return texto.isEmpty() ? null : Long.valueOf(texto);
		} catch (Exception e) {
			return null;
		}
	}

	private String leerTexto(Cell celda) {
		if (celda == null || celda.getCellType() == CellType.BLANK) {
			return null;
		}
		try {
			if (celda.getCellType() == CellType.NUMERIC) {
				double valor = celda.getNumericCellValue();
				return (valor == Math.floor(valor))
						? String.valueOf((long) valor) : String.valueOf(valor);
			}
			String texto = celda.getStringCellValue().trim();
			return texto.isEmpty() ? null : texto;
		} catch (Exception e) {
			return null;
		}
	}
}
