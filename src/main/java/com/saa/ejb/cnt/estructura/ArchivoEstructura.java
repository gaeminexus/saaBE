package com.saa.ejb.cnt.estructura;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Archivo de estructura de la Superintendencia de Bancos (formato de los .txt presentados por ASOPREP):
 * <pre>
 *   cabecera  ESTRUCTURA TAB ENTIDAD TAB dd/MM/aaaa TAB lineas (incluida la cabecera) [TAB suma de control]
 *   una linea por renglon que va al archivo:  codigo TAB valor
 * </pre>
 * Valor con dos decimales y punto, sin separador de miles, "0" cuando es cero; lineas separadas por CRLF y
 * SIN salto al final; codificacion ISO-8859-1. Nombre: ESTRUCTURA + periodicidad + entidad + ddMMaaaa + .txt
 * (B17M396831052025.txt). Es exactamente lo que hace 09-genera-b17-desde-saa.js del expediente del cuadre.
 */
public final class ArchivoEstructura {

	private static final DateTimeFormatter CABECERA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter NOMBRE = DateTimeFormatter.ofPattern("ddMMyyyy");

	private ArchivoEstructura() {
	}

	public static String nombre(DefinicionEstructura def, LocalDate fechaCorte) {
		return def.getCodigoEstructura() + nvl(def.getPeriodicidad()) + nvl(def.getCodigoEntidad())
				+ fechaCorte.format(NOMBRE) + ".txt";
	}

	public static String contenido(DefinicionEstructura def, LocalDate fechaCorte, ResultadoEstructura r) {
		List<String> lineas = new ArrayList<>();
		for (LineaEstructura l : r.getLineas()) {
			if (l.isVaEnArchivo()) {
				lineas.add(l.getCodigo() + "\t" + valor(l.getValor()));
			}
		}
		StringBuilder cab = new StringBuilder();
		cab.append(def.getCodigoEstructura()).append('\t').append(nvl(def.getCodigoEntidad())).append('\t')
				.append(fechaCorte.format(CABECERA)).append('\t').append(lineas.size() + 1);
		if (def.isSumaControl()) {
			cab.append('\t').append(r.getSumaControl().setScale(2).toPlainString());
		}
		lineas.add(0, cab.toString());
		return String.join("\r\n", lineas);
	}

	public static byte[] bytes(String contenido) {
		return contenido.getBytes(StandardCharsets.ISO_8859_1);
	}

	static String valor(BigDecimal v) {
		if (v == null || v.signum() == 0) {
			return "0";
		}
		return v.setScale(2).toPlainString();
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}
}
