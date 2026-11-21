package com.saa.ejb.cnt.flujo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excel con los formatos de ASOPREP (C38, Apache POI): mismas filas, textos, totales (como formulas, igual que el
 * formato) y firmas. Cada libro trae ademas una hoja de controles y, el flujo de caja, una de detalle.
 * <ul>
 * <li>{@link #flujoEfectivo}: "FLUJO DE EFECTIVO_ENE_2026 FORMATO.xlsx" (periodo MES: filas del formato de enero 2026,
 * con la columna G de codigos) o "FLUJO EFECTIVO_12_2025.xlsx" (ACUMULADO: las mismas filas dos mas arriba, sin
 * columna G, como el de diciembre 2025).</li>
 * <li>{@link #flujoCaja}: "FLUJO DE CAJA_12_2025_FORMATO.xlsx" (BIESS): una columna por mes de enero a diciembre.</li>
 * <li>{@link #comparativo}: "COMPARATIVO_01_2026_FORMATO.xlsx".</li>
 * </ul>
 */
public final class ExcelFormatosAsoprep {

	private static final String FMT_PARCIAL = "_ * #,##0.00_ ;_ * \\-#,##0.00_ ;_ * \"-\"??_ ;_ @_ ";
	private static final String FMT_TOTAL = "_(* #,##0.00_);_(* \\(#,##0.00\\);_(* \"-\"??_);_(@_)";
	private static final String FMT_NUMERO = "#,##0.00";
	private static final String[] MESES = { "Enero ", "Febrero ", "Marzo ", "Abril ", "Mayo ", "Junio ", "Julio", "Agosto",
			"Septiembre", "Octubre ", "Noviembre ", "Diciembre" };

	private ExcelFormatosAsoprep() {
	}

	/** Estilos de un libro. */
	private static final class Estilos {
		final CellStyle texto, negrita, titulo, parcial, total, totalNegrita, numero, rojo, firma, fecha, fechaMes, nota;

		Estilos(XSSFWorkbook wb) {
			DataFormat df = wb.createDataFormat();
			Font fNegrita = wb.createFont();
			fNegrita.setBold(true);
			Font fRojo = wb.createFont();
			fRojo.setBold(true);
			fRojo.setColor(IndexedColors.RED.getIndex());
			Font fNota = wb.createFont();
			fNota.setItalic(true);
			fNota.setFontHeightInPoints((short) 9);
			texto = wb.createCellStyle();
			negrita = wb.createCellStyle();
			negrita.setFont(fNegrita);
			titulo = wb.createCellStyle();
			titulo.setFont(fNegrita);
			titulo.setAlignment(HorizontalAlignment.CENTER);
			parcial = wb.createCellStyle();
			parcial.setDataFormat(df.getFormat(FMT_PARCIAL));
			total = wb.createCellStyle();
			total.setDataFormat(df.getFormat(FMT_TOTAL));
			totalNegrita = wb.createCellStyle();
			totalNegrita.setDataFormat(df.getFormat(FMT_TOTAL));
			totalNegrita.setFont(fNegrita);
			numero = wb.createCellStyle();
			numero.setDataFormat(df.getFormat(FMT_NUMERO));
			rojo = wb.createCellStyle();
			rojo.setFont(fRojo);
			firma = wb.createCellStyle();
			firma.setBorderTop(BorderStyle.THIN);
			firma.setAlignment(HorizontalAlignment.CENTER);
			fecha = wb.createCellStyle();
			fecha.setDataFormat(df.getFormat("dd/mm/yyyy"));
			fecha.setAlignment(HorizontalAlignment.LEFT);
			fechaMes = wb.createCellStyle();
			fechaMes.setDataFormat(df.getFormat("mmm-yy"));
			fechaMes.setFont(fNegrita);
			nota = wb.createCellStyle();
			nota.setFont(fNota);
		}
	}

	// ======================================================================================== indirecto

	public static byte[] flujoEfectivo(ResultadoFlujoEfectivo r) throws IOException {
		boolean acumulado = SolicitudFlujoEfectivo.PERIODO_ACUMULADO.equals(r.getPeriodo());
		int d = acumulado ? -2 : 0; // el formato de diciembre 2025 va dos filas mas arriba que el de enero 2026
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Estilos e = new Estilos(wb);
			Sheet h = wb.createSheet("Hoja1");
			ancho(h, 0, 1);
			ancho(h, 1, 47.17);
			ancho(h, 2, 2);
			ancho(h, 3, 16.57);
			ancho(h, 4, 1.67);
			ancho(h, 5, 16.57);
			if (!acumulado) {
				ancho(h, 6, 38.83);
			}
			int filaRazon = acumulado ? 1 : 2, filaTitulo = acumulado ? 2 : 4, filaCorte = acumulado ? 3 : 5;
			int filaPeriodo = acumulado ? 4 : 6;
			texto(h, filaRazon, 1, r.getRazonSocial(), e.titulo);
			texto(h, filaTitulo, 1, r.getTitulo(), e.titulo);
			texto(h, filaCorte, 1, r.getSubtitulo(), e.titulo);
			for (int f : new int[] { filaRazon, filaTitulo, filaCorte }) {
				h.addMergedRegion(new CellRangeAddress(f - 1, f - 1, 1, 5));
			}
			texto(h, filaPeriodo, 1, r.getTextoPeriodo(), e.nota);
			if (r.isPreliminar()) {
				texto(h, filaPeriodo, 3, "PRELIMINAR: mes sin cerrar, no presentable", e.rojo);
			}
			if (!acumulado) {
				texto(h, 7, 6, "DATOS ESTADO  DE SISTUACIÓN FINANCIERA / MENSUAL", e.negrita);
			}
			Map<String, LineaFlujo> porCodigo = new HashMap<>();
			for (LineaFlujo l : r.getLineas()) {
				porCodigo.put(l.getCodigo(), l);
			}
			int ultima = 0;
			for (LineaFlujo l : r.getLineas()) {
				int fila = l.getFila() + d;
				if (l.esTitulo()) {
					texto(h, fila, 1, l.getConcepto(), l.getFila() > 51 ? e.nota : e.negrita);
					continue;
				}
				ultima = Math.max(ultima, fila);
				boolean total = l.getTipoCalculo() == 3;
				texto(h, fila, 1, l.getConcepto(), total ? e.negrita : e.texto);
				int col = columnaIndirecto(l);
				String formula = total ? formulaExcel(l.getFormula(), porCodigo, d) : null;
				CellStyle estilo = total ? e.totalNegrita : (col == 5 ? e.parcial : e.parcial);
				numero(h, fila, col, l.getValor(), formula, estilo);
				if (!acumulado && l.getCodigos() != null) {
					texto(h, fila, 6, l.getCodigos(), e.texto);
				}
			}
			int fn = ultima + 1;
			for (String n : r.getNotas()) {
				texto(h, fn++, 1, "Nota: " + n, e.nota);
			}
			// firmas: contador a la izquierda, representante legal a la derecha (como el formato)
			int ff = acumulado ? 56 : 59;
			firma(h, ff, 1, 1, r.getFirma2Nombre(), r.getFirma2Cargo(), null, e);
			firma(h, ff, 3, 5, r.getFirma1Nombre(), r.getFirma1Cargo(), acumulado ? null : "REPRESENTANTE LEGAL", e);
			hojaControles(wb, e, r.getControles(), r.getAdvertencias(), r.getNotas(), r.getFlujoCaja());
			return cerrar(wb);
		}
	}

	/** Nivel 1 = columna de totales (F); nivel 2 = parciales (D). */
	private static int columnaIndirecto(LineaFlujo l) {
		return l.getNivel() != null && l.getNivel() == 2 ? 3 : 5;
	}

	/** "+FE17 +FE18" -> "+D17+D18" con las celdas de los renglones referidos. */
	private static String formulaExcel(String formula, Map<String, LineaFlujo> porCodigo, int d) {
		StringBuilder sb = new StringBuilder();
		for (Formula.Termino t : Formula.terminos(formula)) {
			LineaFlujo o = porCodigo.get(t.getCodigo());
			if (o == null) {
				return null;
			}
			sb.append(t.getSigno() < 0 ? "-" : "+").append(columna(columnaIndirecto(o))).append(o.getFila() + d);
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	// ======================================================================================== directo (BIESS)

	public static byte[] flujoCaja(ResultadoFlujoEfectivo r) throws IOException {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Estilos e = new Estilos(wb);
			Sheet h = wb.createSheet("Hoja1");
			ancho(h, 0, 1);
			ancho(h, 1, 41);
			for (int c = 2; c <= 13; c++) {
				ancho(h, c, 13.5);
			}
			ancho(h, 14, 22);
			texto(h, 1, 1, "NOMBRE COMPLETO DEL FONDO:  ", e.negrita);
			texto(h, 1, 2, r.getRazonSocial(), e.negrita);
			h.addMergedRegion(new CellRangeAddress(0, 0, 2, 7));
			texto(h, 2, 1, "Código entidad", e.negrita);
			if (r.getCodigoEntidad() != null && r.getCodigoEntidad().matches("\\d+")) {
				celda(h, 2, 2).setCellValue(Long.parseLong(r.getCodigoEntidad()));
			} else {
				texto(h, 2, 2, r.getCodigoEntidad(), e.texto);
			}
			texto(h, 3, 1, "Fondo Tipo  Tamaño de Activos:  (I,II,III)", e.negrita);
			texto(h, 4, 1, "ACTIVOS TOTALES (0,00 US dólares)", e.negrita);
			numero(h, 4, 2, r.getActivosTotales(), null, e.parcial);
			texto(h, 5, 1, "Fecha corte:", e.negrita);
			Cell fc = celda(h, 5, 2);
			fc.setCellValue(Date.from(r.getFechaCorte().atStartOfDay(ZoneId.systemDefault()).toInstant()));
			fc.setCellStyle(e.fecha);
			if (r.isPreliminar()) {
				texto(h, 5, 4, "PRELIMINAR: mes sin cerrar, no presentable", e.rojo);
			}
			texto(h, 7, 1, "FORMATO FLUJO DE CAJA", e.negrita);
			texto(h, 7, 2, "REAL ", e.titulo);
			h.addMergedRegion(new CellRangeAddress(6, 6, 2, 13));
			texto(h, 8, 1, "CUENTAS", e.negrita);
			h.addMergedRegion(new CellRangeAddress(7, 8, 1, 1));
			for (int m = 0; m < 12; m++) {
				texto(h, 8, 2 + m, MESES[m], e.titulo);
				h.addMergedRegion(new CellRangeAddress(7, 8, 2 + m, 2 + m));
			}
			texto(h, 8, 14, "Cuentas", e.negrita);

			// filas: de la definicion (la del primer mes; todas tienen las mismas)
			List<LineaFlujo> filas = r.getFlujoCaja().isEmpty() ? java.util.Collections.emptyList() : r.getFlujoCaja().get(0).getLineas();
			Map<String, LineaFlujo> porCodigo = new HashMap<>();
			for (LineaFlujo l : filas) {
				porCodigo.put(l.getCodigo(), l);
			}
			int ultima = 0;
			for (LineaFlujo l : filas) {
				ultima = Math.max(ultima, l.getFila());
				boolean total = l.getTipoCalculo() == 3;
				texto(h, l.getFila(), 1, l.getConcepto(), l.esTitulo() || total ? e.negrita : e.texto);
				if (l.getCodigos() != null) {
					texto(h, l.getFila(), 14, l.getCodigos(), e.texto);
				} else if (!l.esTitulo() && l.getValor() == null && l.getTipoCalculo() != 3
						&& !CalculadorFlujoDirecto.RENGLON_SALDO_INICIAL.equals(l.getCodigo())) {
					texto(h, l.getFila(), 14, "no sale de la contabilidad", e.nota);
				}
			}
			for (FlujoCajaMes m : r.getFlujoCaja()) {
				int col = 1 + m.getMes();
				String letra = columna(col);
				for (LineaFlujo l : m.getLineas()) {
					if (l.esTitulo() || l.getValor() == null && l.getTipoCalculo() != 3) {
						continue;
					}
					boolean total = l.getTipoCalculo() == 3;
					String formula = null;
					if (total) {
						StringBuilder sb = new StringBuilder();
						for (Formula.Termino t : Formula.terminos(l.getFormula())) {
							LineaFlujo o = porCodigo.get(t.getCodigo());
							if (o == null) {
								sb.setLength(0);
								break;
							}
							sb.append(t.getSigno() < 0 ? "-" : "+").append(letra).append(o.getFila());
						}
						formula = sb.length() == 0 ? null : sb.toString();
					}
					numero(h, l.getFila(), col, l.getValor(), formula, total ? e.totalNegrita : e.numero);
				}
			}
			int fn = Math.max(ultima + 2, 40);
			texto(h, fn++, 1, "Notas Generales ( explicaciones de flujo)", e.negrita);
			texto(h, fn++, 1, "Flujo de caja real: movimientos de la caja (cuentas de la regla del saldo inicial) clasificados por su"
					+ " contrapartida; el dinero que pasa por una cuenta puente se sigue hasta la cuenta final con lo que el puente"
					+ " liquidó en el mes. Detalle por cuenta en la hoja Detalle.", e.nota);
			for (String n : r.getNotas()) {
				texto(h, fn++, 1, "Nota: " + n, e.nota);
			}
			for (FlujoCajaMes m : r.getFlujoCaja()) {
				for (ControlFlujo c : m.getControles()) {
					if (!c.isOk()) {
						texto(h, fn++, 1, c.getEstado() + " " + c.getNombre() + ": " + c.getValor() + ". " + c.getDetalle(), e.nota);
					}
				}
			}
			int ff = Math.max(fn + 2, 60);
			firma(h, ff, 1, 1, r.getFirma2Nombre(), r.getFirma2Cargo(), null, e);
			firma(h, ff, 4, 6, r.getFirma1Nombre(), r.getFirma1Cargo(), null, e);
			hojaDetalle(wb, e, r.getFlujoCaja());
			hojaControles(wb, e, r.getControles(), r.getAdvertencias(), r.getNotas(), r.getFlujoCaja());
			return cerrar(wb);
		}
	}

	// ======================================================================================== comparativo

	public static byte[] comparativo(ResultadoComparativo r) throws IOException {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Estilos e = new Estilos(wb);
			Sheet h = wb.createSheet(String.valueOf(r.getAnio()));
			ancho(h, 0, 0.5);
			ancho(h, 1, 8);
			ancho(h, 2, 46.21);
			ancho(h, 3, 0.79);
			ancho(h, 4, 14.5);
			ancho(h, 5, 0.79);
			ancho(h, 6, 14.5);
			ancho(h, 7, 14.5);
			ancho(h, 8, 6);
			ancho(h, 9, 9);
			texto(h, 1, 1, r.getRazonSocial(), e.titulo);
			texto(h, 2, 1, r.getTitulo(), e.titulo);
			h.addMergedRegion(new CellRangeAddress(0, 0, 1, 6));
			h.addMergedRegion(new CellRangeAddress(1, 1, 1, 6));
			if (r.isPreliminar()) {
				texto(h, 3, 1, "PRELIMINAR: mes sin cerrar, no presentable", e.rojo);
			}
			texto(h, 4, 1, "CODIGO", e.negrita);
			texto(h, 4, 2, "CUENTA", e.negrita);
			fechaMes(h, 4, 4, r.getFechaAnterior(), e);
			fechaMes(h, 4, 6, r.getFechaCorte(), e);
			texto(h, 5, 9, "FUENTE", e.negrita);
			int ultima = 4;
			for (LineaComparativo l : r.getLineas()) {
				ultima = Math.max(ultima, l.getFila());
				if (l.getCodigo().matches("\\d+")) {
					celda(h, l.getFila(), 1).setCellValue(Long.parseLong(l.getCodigo()));
				} else {
					texto(h, l.getFila(), 1, l.getCodigo(), e.texto);
				}
				texto(h, l.getFila(), 2, l.getNombre(), l.isConValores() ? e.texto : e.negrita);
				if (!l.isConValores()) {
					continue;
				}
				numero(h, l.getFila(), 4, l.getValorAnterior(), null, e.numero);
				numero(h, l.getFila(), 6, l.getValorActual(), null, e.numero);
				String f = l.isActivo() ? "E" + l.getFila() + "-G" + l.getFila() : "-E" + l.getFila() + "+G" + l.getFila();
				numero(h, l.getFila(), 7, l.getEfecto(), f, e.total);
				if (l.getFuente() != null && !l.getFuente().isEmpty()) {
					texto(h, l.getFila(), 8, l.getFuente(), e.texto);
				}
			}
			int fn = ultima + 2;
			texto(h, fn++, 2, "Columna H: efecto en caja (activo = cierre anterior - mes; pasivo y patrimonio = mes - cierre anterior).", e.nota);
			for (String n : r.getNotas()) {
				texto(h, fn++, 2, "Nota: " + n, e.nota);
			}
			hojaControles(wb, e, r.getControles(), r.getAdvertencias(), r.getNotas(), null);
			return cerrar(wb);
		}
	}

	// ======================================================================================== hojas auxiliares

	private static void hojaControles(XSSFWorkbook wb, Estilos e, List<ControlFlujo> controles, List<String> advertencias,
			List<String> notas, List<FlujoCajaMes> meses) {
		Sheet h = wb.createSheet("Controles");
		ancho(h, 0, 12);
		ancho(h, 1, 70);
		ancho(h, 2, 16);
		ancho(h, 3, 16);
		ancho(h, 4, 14);
		ancho(h, 5, 13);
		ancho(h, 6, 80);
		String[] cab = { "Grupo", "Control", "Valor", "Esperado", "Diferencia", "Estado", "Detalle" };
		for (int i = 0; i < cab.length; i++) {
			texto(h, 1, i, cab[i], e.negrita);
		}
		int f = 2;
		java.util.List<ControlFlujo> todos = new java.util.ArrayList<>(controles);
		if (meses != null) {
			for (FlujoCajaMes m : meses) {
				todos.addAll(m.getControles());
			}
		}
		for (ControlFlujo c : todos) {
			texto(h, f, 0, c.getGrupo(), e.texto);
			texto(h, f, 1, c.getNombre(), e.texto);
			numero(h, f, 2, c.getValor(), null, e.numero);
			numero(h, f, 3, c.getEsperado(), null, e.numero);
			numero(h, f, 4, c.getDiferencia(), null, e.numero);
			texto(h, f, 5, c.getEstado(), ControlFlujo.ERROR.equals(c.getEstado()) ? e.rojo : e.negrita);
			texto(h, f, 6, c.getDetalle(), e.texto);
			f++;
		}
		f++;
		for (String a : advertencias) {
			texto(h, f++, 1, "Advertencia: " + a, e.texto);
		}
		for (String n : notas) {
			texto(h, f++, 1, "Nota: " + n, e.texto);
		}
	}

	private static void hojaDetalle(XSSFWorkbook wb, Estilos e, List<FlujoCajaMes> meses) {
		Sheet h = wb.createSheet("Detalle");
		ancho(h, 0, 9);
		ancho(h, 1, 7);
		ancho(h, 2, 18);
		ancho(h, 3, 40);
		ancho(h, 4, 34);
		ancho(h, 5, 16);
		String[] cab = { "Mes", "Fila", "Cuenta del SAA", "Codigo B17 / puente", "Via (puentes recorridos)", "Efecto en caja" };
		for (int i = 0; i < cab.length; i++) {
			texto(h, 1, i, cab[i], e.negrita);
		}
		int f = 2;
		for (FlujoCajaMes m : meses) {
			String mes = String.format("%d-%02d", m.getAnio(), m.getMes());
			for (DetalleFlujoCaja x : m.getDetalle()) {
				texto(h, f, 0, mes, e.texto);
				texto(h, f, 1, x.getFila(), e.texto);
				texto(h, f, 2, x.getCuenta(), e.texto);
				texto(h, f, 3, x.getHojaB17(), e.texto);
				texto(h, f, 4, x.getVia(), e.texto);
				numero(h, f, 5, x.getValor(), null, e.numero);
				f++;
			}
		}
		f++;
		String[] cabP = { "Mes", "Puente", "Nombre", "Abonado (entra)", "Debitado (sale)", "Asientos de liquidacion",
				"Sin liquidacion", "Ciclo" };
		for (int i = 0; i < cabP.length; i++) {
			texto(h, f, i, cabP[i], e.negrita);
		}
		f++;
		for (FlujoCajaMes m : meses) {
			String mes = String.format("%d-%02d", m.getAnio(), m.getMes());
			for (UsoPuente u : m.getPuentes()) {
				texto(h, f, 0, mes, e.texto);
				texto(h, f, 1, u.getCodigo(), e.texto);
				texto(h, f, 2, u.getNombre(), e.texto);
				numero(h, f, 3, u.getAbonado(), null, e.numero);
				numero(h, f, 4, u.getDebitado(), null, e.numero);
				celda(h, f, 5).setCellValue(u.getAsientosLiquidacion());
				numero(h, f, 6, u.getSinLiquidacion(), null, e.numero);
				numero(h, f, 7, u.getCiclo(), null, e.numero);
				f++;
			}
		}
	}

	// ======================================================================================== utilidades

	private static void firma(Sheet h, int fila, int desde, int hasta, String nombre, String cargo, String tercera, Estilos e) {
		texto(h, fila, desde, nombre == null ? "" : nombre, e.firma);
		texto(h, fila + 1, desde, cargo == null ? "" : cargo, e.titulo);
		if (tercera != null) {
			texto(h, fila + 2, desde, tercera, e.titulo);
		}
		if (hasta > desde) {
			for (int c = desde + 1; c <= hasta; c++) {
				celda(h, fila, c).setCellStyle(e.firma);
			}
			for (int f = fila; f <= fila + (tercera == null ? 1 : 2); f++) {
				h.addMergedRegion(new CellRangeAddress(f - 1, f - 1, desde, hasta));
			}
		}
	}

	private static void fechaMes(Sheet h, int fila, int col, java.time.LocalDate d, Estilos e) {
		Cell c = celda(h, fila, col);
		c.setCellValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		c.setCellStyle(e.fechaMes);
	}

	private static byte[] cerrar(XSSFWorkbook wb) throws IOException {
		XSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
		wb.setForceFormulaRecalculation(true);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		wb.write(out);
		return out.toByteArray();
	}

	private static void ancho(Sheet h, int col, double caracteres) {
		h.setColumnWidth(col, (int) Math.round(caracteres * 256));
	}

	/** Celda por fila del Excel (1 = primera) y columna (0 = A). */
	private static Cell celda(Sheet h, int fila, int col) {
		Row row = h.getRow(fila - 1);
		if (row == null) {
			row = h.createRow(fila - 1);
		}
		Cell c = row.getCell(col);
		return c == null ? row.createCell(col) : c;
	}

	private static void texto(Sheet h, int fila, int col, String v, CellStyle estilo) {
		Cell c = celda(h, fila, col);
		c.setCellValue(v == null ? "" : v);
		c.setCellStyle(estilo);
	}

	private static void numero(Sheet h, int fila, int col, BigDecimal v, String formula, CellStyle estilo) {
		Cell c = celda(h, fila, col);
		if (formula != null) {
			c.setCellFormula(formula.startsWith("+") ? formula.substring(1) : formula);
		} else if (v != null) {
			c.setCellValue(v.doubleValue());
		}
		c.setCellStyle(estilo);
	}

	private static String columna(int col) {
		return org.apache.poi.ss.util.CellReference.convertNumToColString(col);
	}
}
