package com.saa.ejb.tsr.formateador;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.saa.basico.util.IncomeException;
import com.saa.model.cxp.LotePago;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.tsr.BancoExterno;
import com.saa.rubros.TipoCuentasBancarias;
import com.saa.rubros.TipoIdentificacion;

/**
 * Data para pegar en la hoja de detalle de la macro BizBank Light v2.1 del
 * Banco del Pacífico. Ver
 * docs/logica-negocio/pagos/FORMATO-ARCHIVO-BANCOS.md §2.
 *
 * ⛔ Esto NO es el archivo que recibe el banco: la macro de Excel es la que
 * genera el {@code .BCP} a partir de esta hoja. Nuestro entregable es el
 * {@code .xlsx}. Sin fila de encabezado: la data arranca en la fila 1
 * (índice 0 de POI) porque el usuario la pega en la fila 16 de
 * {@code BIZBANK_LIGHT.xls}.
 */
public class PacificoArchivoPagoFormateador implements FormateadorArchivoPagos {

	@Override
	public ArchivoPagosGenerado generar(LotePago lote, List<PagoProgramado> pagos) throws Throwable {

		System.out.println("=== PacificoArchivoPagoFormateador | lote=" + lote.getId()
				+ " | pagos=" + (pagos != null ? pagos.size() : 0) + " ===");

		if (pagos == null || pagos.isEmpty()) {
			throw new IncomeException("El lote no tiene pagos que incluir en el archivo.");
		}

		byte[] contenido;
		try (XSSFWorkbook libro = new XSSFWorkbook()) {
			Sheet hoja = libro.createSheet("Detalle");

			int numeroFila = 0;
			for (PagoProgramado pago : pagos) {
				escribirFila(hoja.createRow(numeroFila++), pago);
			}

			ByteArrayOutputStream salida = new ByteArrayOutputStream();
			libro.write(salida);
			contenido = salida.toByteArray();
		}

		ArchivoPagosGenerado archivo = new ArchivoPagosGenerado();
		archivo.setContenido(contenido);
		archivo.setTextoPlano(null);
		archivo.setNombreArchivo(nombreArchivo(lote));
		archivo.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		archivo.setFormatoBanco("PACIFICO");
		return archivo;
	}

	private void escribirFila(Row fila, PagoProgramado pago) {

		boolean tieneCuentaTitular = (pago.getCuentaDestino() != null);
		boolean tieneBeneficiarioOcasional = (pago.getBeneficiarioCuenta() != null
				&& !pago.getBeneficiarioCuenta().trim().isEmpty());

		if (!tieneCuentaTitular && !tieneBeneficiarioOcasional) {
			throw new IncomeException("El pago " + pago.getId() + " (beneficiario "
					+ nombreBeneficiario(pago) + ") no tiene cuenta bancaria de destino registrada. "
					+ "Registre la cuenta del beneficiario antes de generar el archivo.");
		}

		String identificacion;
		String tipoIdentificacion;
		Long tipoCuentaRubro;
		String numeroCuenta;
		BancoExterno banco;

		if (tieneCuentaTitular) {
			identificacion = nvl(pago.getTitular() != null ? pago.getTitular().getIdentificacion() : null);
			Long rubroTipoId = (pago.getTitular() != null) ? pago.getTitular().getRubroTipoIdentificacionP() : null;
			tipoIdentificacion = (rubroTipoId != null)
					? letraTipoIdentificacion(rubroTipoId.intValue(), pago)
					: tipoIdentificacionPorLongitud(identificacion);
			tipoCuentaRubro = pago.getCuentaDestino().getTipoCuenta();
			numeroCuenta = nvl(pago.getCuentaDestino().getNumeroCuenta());
			banco = pago.getCuentaDestino().getBanco();
		} else {
			identificacion = nvl(pago.getBeneficiarioIdentificacion());
			tipoIdentificacion = tipoIdentificacionPorLongitud(identificacion);
			tipoCuentaRubro = pago.getBeneficiarioTipoCuenta();
			numeroCuenta = nvl(pago.getBeneficiarioCuenta());
			banco = pago.getBeneficiarioBanco();
		}

		String codigoBanco = CodigoBancoBeneficiarioResolver.codigoBancoBeneficiario(
				banco, pago.getId(), nombreBeneficiario(pago));

		celda(fila, 0, "CU");                                          // A: Forma Pag/Cob
		celda(fila, 1, codigoBanco);                                   // B: Banco (codigo del beneficiario)
		celda(fila, 2, tipoCuenta(tipoCuentaRubro, pago));             // C: Tip.Cta/Che
		celda(fila, 3, numeroCuenta);                                  // D: Num.Cta/Che
		// COLUMNA E, VALOR: EN DOLARES CON DECIMALES (450.00) -- AL REVES QUE EL
		// INTERNACIONAL, que va en centavos corridos. La misma persona usa las
		// dos pantallas el mismo dia: confundir el formato multiplica o divide
		// por cien el lote entero.
		fila.createCell(4).setCellValue(valorEnDolares(pago));         // E: Valor
		// F: Identificacion -- vacia, solo obligatoria en ordenes de cobro
		celda(fila, 6, tipoIdentificacion);                            // G: Tip.Doc
		celda(fila, 7, soloDigitosYLetras(identificacion));            // H: NUC
		celda(fila, 8, nombreBeneficiario(pago));                      // I: Beneficiario
		// J: Telefono -- vacia
		celda(fila, 10, sanearReferencia(nvl(pago.getObservacion()))); // K: Referencia
		// L, M, N: vacias -- Base Imponible/Base IVA/Tipo son solo de recaudacion
	}

	private void celda(Row fila, int indiceColumna, String valor) {
		fila.createCell(indiceColumna).setCellValue(valor != null ? valor : "");
	}

	private double valorEnDolares(PagoProgramado pago) {
		if (pago.getValor() == null || pago.getValor() <= 0) {
			throw new IncomeException("El pago " + pago.getId() + " no tiene un valor mayor que cero.");
		}
		return pago.getValor();
	}

	private String tipoCuenta(Long tipoCuentaRubro, PagoProgramado pago) {
		if (tipoCuentaRubro == null) {
			throw new IncomeException("El pago " + pago.getId() + " no tiene tipo de cuenta del beneficiario.");
		}
		if (tipoCuentaRubro.intValue() == TipoCuentasBancarias.CORRIENTE) {
			return "00";
		}
		if (tipoCuentaRubro.intValue() == TipoCuentasBancarias.AHORROS) {
			return "10";
		}
		throw new IncomeException("El pago " + pago.getId() + " tiene un tipo de cuenta ("
				+ tipoCuentaRubro + ") sin equivalente en el formato del Banco del Pacifico.");
	}

	/**
	 * Deducción por longitud para el BENEFICIARIO OCASIONAL: 10 dígitos ⇒ C,
	 * 13 ⇒ R, cualquier otra cosa ⇒ P. Misma regla que usa el propio Banco
	 * Internacional para validar su campo 10
	 * (FORMATO-ARCHIVO-BANCOS.md §4.1), no una invención nuestra.
	 */
	private String tipoIdentificacionPorLongitud(String identificacion) {
		String soloDigitos = identificacion.replaceAll("\\D", "");
		if (soloDigitos.length() == 10) {
			return "C";
		}
		if (soloDigitos.length() == 13) {
			return "R";
		}
		return "P";
	}

	private String letraTipoIdentificacion(int rubro, PagoProgramado pago) {
		switch (rubro) {
			case TipoIdentificacion.CEDULA_IDENTIDAD:
				return "C";
			case TipoIdentificacion.RUC:
				return "R";
			case TipoIdentificacion.PASAPORTE:
				return "P";
			case TipoIdentificacion.IDENTIFICACION_DEL_EXTERIOR:
				throw new IncomeException("El pago " + pago.getId() + " (beneficiario "
						+ nombreBeneficiario(pago) + ") tiene identificacion del exterior, que no "
						+ "tiene equivalente en el formato del Banco del Pacifico.");
			default:
				throw new IncomeException("El pago " + pago.getId() + " tiene un tipo de identificacion ("
						+ rubro + ") no reconocido.");
		}
	}

	private String soloDigitosYLetras(String identificacion) {
		return identificacion.replaceAll("\\s+", "");
	}

	/**
	 * Columna K: la macro rechaza ñ, coma, punto, punto y coma, guion y
	 * barra. Se sanea acá — no se confía en que la observación llegue limpia
	 * (FORMATO-ARCHIVO-BANCOS.md §2.5).
	 */
	private String sanearReferencia(String texto) {
		String saneado = texto
				.replace("ñ", "n").replace("Ñ", "N")
				.replaceAll("[,.;\\-/]", " ");
		return saneado.replaceAll("\\s+", " ").trim();
	}

	private String nombreBeneficiario(PagoProgramado pago) {
		if (pago.getTitular() != null) {
			return nvl(pago.getTitular().getNombre());
		}
		return nvl(pago.getBeneficiarioNombre());
	}

	private String nvl(String valor) {
		return (valor != null) ? valor.trim() : "";
	}

	private String nombreArchivo(LotePago lote) {
		String fecha = (lote.getFechaGeneracion() != null)
				? lote.getFechaGeneracion().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
				: "";
		return "PAGOS_PACIFICO_LOTE_" + lote.getId() + "_" + fecha + ".xlsx";
	}
}
