package com.saa.ejb.tsr.formateador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.model.cxp.LotePago;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.tsr.BancoExterno;
import com.saa.rubros.TipoCuentasBancarias;
import com.saa.rubros.TipoIdentificacion;

/**
 * Formato "CORTO (pagos, recaudaciones)" del Banco Internacional. Ver
 * docs/logica-negocio/pagos/FORMATO-ARCHIVO-BANCOS.md §1.
 *
 * Texto separado por TABULADOR, SIN fila de encabezado, codificación ANSI
 * (windows-1252, no UTF-8), salto de línea \r\n. Doce campos fijos: un
 * campo opcional vacío igual lleva su tabulador — once tabuladores siempre.
 */
public class InternacionalArchivoPagoFormateador implements FormateadorArchivoPagos {

	private static final String TAB = "\t";
	private static final Charset ANSI = Charset.forName("windows-1252");
	private static final int LARGO_MAXIMO_NOMBRE = 41;

	@Override
	public ArchivoPagosGenerado generar(LotePago lote, List<PagoProgramado> pagos) throws Throwable {

		System.out.println("=== InternacionalArchivoPagoFormateador | lote=" + lote.getId()
				+ " | pagos=" + (pagos != null ? pagos.size() : 0) + " ===");

		if (pagos == null || pagos.isEmpty()) {
			throw new IncomeException("El lote no tiene pagos que incluir en el archivo.");
		}

		StringBuilder texto = new StringBuilder();
		for (PagoProgramado pago : pagos) {
			texto.append(lineaPago(pago)).append("\r\n");
		}

		ArchivoPagosGenerado archivo = new ArchivoPagosGenerado();
		archivo.setTextoPlano(texto.toString());
		archivo.setContenido(texto.toString().getBytes(ANSI));
		archivo.setNombreArchivo(nombreArchivo(lote));
		archivo.setMimeType("text/plain");
		archivo.setFormatoBanco("INTERNACIONAL");
		return archivo;
	}

	private String lineaPago(PagoProgramado pago) {

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

		// Campo 12: un codigo vacio significa "32" (Banco Internacional) para el
		// banco, asi que nunca se manda vacio -- aborta la linea en vez de arriesgar
		// una transferencia a un banco que no es el del beneficiario.
		String codigoBanco = CodigoBancoBeneficiarioResolver.codigoBancoBeneficiario(
				banco, pago.getId(), nombreBeneficiario(pago));

		String[] campos = new String[12];
		campos[0] = "PA";
		// Campo 2, Contrapartida: identificador de la transaccion, no puede ir
		// vacio. FORMATO-ARCHIVO-BANCOS.md #4 no dice de donde sale este campo
		// puntual -- se usa el id del pago (PGS.PGTR.PGTRCDGO), unico y nunca
		// vacio. Reportado al arbitro como algo a confirmar, no asumido en silencio.
		campos[1] = String.valueOf(pago.getId());
		campos[2] = "USD";
		campos[3] = valorEnCentavos(pago);
		campos[4] = "CTA";
		campos[5] = tipoCuenta(tipoCuentaRubro, pago);
		campos[6] = numeroCuenta;
		campos[7] = nvl(pago.getObservacion());
		campos[8] = tipoIdentificacion;
		campos[9] = soloDigitosYLetras(identificacion);
		campos[10] = truncar(nombreBeneficiario(pago), LARGO_MAXIMO_NOMBRE);
		campos[11] = codigoBanco;

		return String.join(TAB, campos);
	}

	/**
	 * CAMPO 4: el valor va en centavos corridos, sin separador decimal.
	 * 2458.79 se escribe 245879. Es el error más caro posible de este
	 * archivo: con punto decimal, el banco transfiere centavos, no dólares.
	 */
	private String valorEnCentavos(PagoProgramado pago) {
		if (pago.getValor() == null || pago.getValor() <= 0) {
			throw new IncomeException("El pago " + pago.getId() + " no tiene un valor mayor que cero.");
		}
		BigDecimal centavos = BigDecimal.valueOf(pago.getValor())
				.setScale(2, RoundingMode.HALF_UP)
				.movePointRight(2)
				.setScale(0, RoundingMode.HALF_UP);
		return centavos.toPlainString();
	}

	private String tipoCuenta(Long tipoCuentaRubro, PagoProgramado pago) {
		if (tipoCuentaRubro == null) {
			throw new IncomeException("El pago " + pago.getId() + " no tiene tipo de cuenta del beneficiario.");
		}
		if (tipoCuentaRubro.intValue() == TipoCuentasBancarias.CORRIENTE) {
			return "CTE";
		}
		if (tipoCuentaRubro.intValue() == TipoCuentasBancarias.AHORROS) {
			return "AHO";
		}
		throw new IncomeException("El pago " + pago.getId() + " tiene un tipo de cuenta ("
				+ tipoCuentaRubro + ") sin equivalente en el formato del Banco Internacional.");
	}

	/**
	 * Deducción por longitud para el BENEFICIARIO OCASIONAL, que no guarda tipo
	 * de identificación: 10 dígitos ⇒ C (cédula), 13 ⇒ R (RUC), cualquier otra
	 * cosa ⇒ P (pasaporte). No es un invento nuestro: es la misma regla con la
	 * que el propio Banco Internacional valida el campo 10 (ver
	 * FORMATO-ARCHIVO-BANCOS.md §1.2 y §4.1), leída al revés.
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
						+ "tiene equivalente en el formato del Banco Internacional.");
			default:
				throw new IncomeException("El pago " + pago.getId() + " tiene un tipo de identificacion ("
						+ rubro + ") no reconocido.");
		}
	}

	private String soloDigitosYLetras(String identificacion) {
		// "Sin espacios intermedios" (FORMATO-ARCHIVO-BANCOS.md §1.2, campo 10).
		return identificacion.replaceAll("\\s+", "");
	}

	private String nombreBeneficiario(PagoProgramado pago) {
		if (pago.getTitular() != null) {
			return nvl(pago.getTitular().getNombre());
		}
		return nvl(pago.getBeneficiarioNombre());
	}

	private String truncar(String texto, int largoMaximo) {
		return (texto.length() > largoMaximo) ? texto.substring(0, largoMaximo) : texto;
	}

	private String nvl(String valor) {
		return (valor != null) ? valor.trim() : "";
	}

	private String nombreArchivo(LotePago lote) {
		String fecha = (lote.getFechaGeneracion() != null)
				? lote.getFechaGeneracion().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
				: "";
		return "PAGOS_LOTE_" + lote.getId() + "_" + fecha + ".txt";
	}
}
