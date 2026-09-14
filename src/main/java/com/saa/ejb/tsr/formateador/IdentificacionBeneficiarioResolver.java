package com.saa.ejb.tsr.formateador;

import com.saa.basico.util.IncomeException;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.tsr.CuentaBancariaTitular;
import com.saa.model.tsr.Titular;
import com.saa.rubros.TipoIdentificacion;

/**
 * Resuelve el tipo y el número de identificación del beneficiario que van al
 * archivo de pagos, detrás de un único método -- mismo motivo que
 * {@link CodigoBancoBeneficiarioResolver}: que la especificación bancaria
 * cambie una vez y no en los dos formateadores. Ver
 * docs/logica-negocio/tsr/API-IDENTIFICACION-CUENTA-BANCARIA.md §2.2.
 *
 * <p>Regla, para un pago con {@code cuentaDestino} (cuenta de un titular):
 * <ol>
 *   <li>Si la cuenta tiene su propia {@code tipoIdentificacion}/{@code identificacion}
 *       (con la que se abrió en el banco): van esos dos.</li>
 *   <li>Si no: la del titular, exactamente como antes de este resolver (movido tal
 *       cual desde {@code InternacionalArchivoPagoFormateador:74-92}, ítem 11 del
 *       2026-09-09) -- por {@code rubroTipoIdentificacionH} sólo si el par P/H de
 *       {@code Titular} no es nulo, si no por longitud.</li>
 * </ol>
 * Para un pago sin {@code cuentaDestino} (beneficiario ocasional), la identificación
 * es la denormalizada del pago (PGTRBFID): no cambia.
 *
 * <p>Precondición del llamador: exactamente uno de {@code pago.getCuentaDestino()} o
 * el beneficiario ocasional (PGTRBFCT no vacío) está poblado -- lo garantizan ambos
 * formateadores con su propia guarda antes de llegar acá, así que este resolver no
 * la repite.
 *
 * <p>⚠️ {@code validarLongitudIdentificacion} NO vive acá a propósito: hoy sólo el
 * Internacional la llama -- el Pacífico nunca validó la longitud de la
 * identificación. Moverla acá y llamarla desde los dos le agregaría una validación
 * nueva al Pacífico que no tiene hoy. Se queda como método privado de
 * {@code InternacionalArchivoPagoFormateador}, sin tocar.
 */
public final class IdentificacionBeneficiarioResolver {

	private IdentificacionBeneficiarioResolver() {
	}

	/**
	 * Tipo (letra {@code "C"}/{@code "R"}/{@code "P"}) y número de identificación,
	 * ya sin espacios, listos para escribir en el archivo del banco.
	 */
	public static final class Resultado {
		private final String tipo;
		private final String identificacion;

		private Resultado(String tipo, String identificacion) {
			this.tipo = tipo;
			this.identificacion = identificacion;
		}

		public String getTipo() {
			return tipo;
		}

		public String getIdentificacion() {
			return identificacion;
		}
	}

	/**
	 * @param pago               : Pago con {@code cuentaDestino} poblado (cuenta de un
	 *                             titular) o con beneficiario ocasional (PGTRBFCT no vacío)
	 * @param nombreFormatoBanco : Nombre del banco, para el mensaje de error de
	 *                             identificación del exterior (ej. "Banco Internacional",
	 *                             "Banco del Pacifico") -- cada formato bancario lo nombraba
	 *                             distinto y ese texto no cambia
	 * @return                   : Tipo e identificación a usar en el archivo
	 * @throws IncomeException   : Si el tipo de identificación es del exterior o no se
	 *                             reconoce
	 */
	public static Resultado resolver(PagoProgramado pago, String nombreFormatoBanco) {
		boolean tieneCuentaTitular = (pago.getCuentaDestino() != null);

		String identificacion;
		String tipo;

		if (tieneCuentaTitular) {
			CuentaBancariaTitular cuenta = pago.getCuentaDestino();
			if (cuenta.getTipoIdentificacion() != null && cuenta.getIdentificacion() != null
					&& !cuenta.getIdentificacion().trim().isEmpty()) {
				// La cuenta tiene su propia identificación -- la que se usó para
				// abrirla, no necesariamente la del titular (API-IDENTIFICACION-
				// CUENTA-BANCARIA.md §2.2.1).
				identificacion = nvl(cuenta.getIdentificacion());
				tipo = letra(cuenta.getTipoIdentificacion().intValue(), pago, nombreFormatoBanco);
			} else {
				// Sin identificación propia: la del titular, tal cual antes de este
				// resolver (§2.2.2).
				Titular titular = pago.getTitular();
				identificacion = nvl(titular != null ? titular.getIdentificacion() : null);
				// ÍTEM 11 (2026-09-09): TSR.TTLR guarda el tipo de identificación en DOS
				// columnas (Titular.java:110-122) -- rubroTipoIdentificacionP (TTLRRYYB) es
				// el rubro PADRE (36, SIEMPRE ese valor para todos los titulares); el
				// detalle real (1=CEDULA, 2=RUC, 3=PASAPORTE, 4=EXTERIOR) está en
				// rubroTipoIdentificacionH (TTLRRZZB, el HIJO). Exigir las DOS no nulas
				// antes de confiar en la H; si falta cualquiera, respaldo por longitud.
				boolean tipoIdentificacionConfiable = (titular != null)
						&& (titular.getRubroTipoIdentificacionP() != null)
						&& (titular.getRubroTipoIdentificacionH() != null);
				tipo = tipoIdentificacionConfiable
						? letra(titular.getRubroTipoIdentificacionH().intValue(), pago, nombreFormatoBanco)
						: porLongitud(identificacion);
			}
		} else {
			// Beneficiario ocasional: no cambia.
			identificacion = nvl(pago.getBeneficiarioIdentificacion());
			tipo = porLongitud(identificacion);
		}

		return new Resultado(tipo, soloDigitosYLetras(identificacion));
	}

	private static String letra(int rubro, PagoProgramado pago, String nombreFormatoBanco) {
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
						+ "tiene equivalente en el formato del " + nombreFormatoBanco + ".");
			default:
				throw new IncomeException("El pago " + pago.getId() + " (beneficiario " + nombreBeneficiario(pago)
						+ ") tiene un tipo de identificacion (" + rubro + ") no reconocido.");
		}
	}

	/**
	 * Deducción por longitud: 10 dígitos ⇒ C (cédula), 13 ⇒ R (RUC), cualquier otra
	 * cosa ⇒ P (pasaporte). Misma regla que usa el propio Banco Internacional para
	 * validar su campo 10 (FORMATO-ARCHIVO-BANCOS.md §1.2 y §4.1), leída al revés.
	 */
	private static String porLongitud(String identificacion) {
		String soloDigitos = identificacion.replaceAll("\\D", "");
		if (soloDigitos.length() == 10) {
			return "C";
		}
		if (soloDigitos.length() == 13) {
			return "R";
		}
		return "P";
	}

	private static String soloDigitosYLetras(String identificacion) {
		// "Sin espacios intermedios" (FORMATO-ARCHIVO-BANCOS.md §1.2, campo 10).
		return identificacion.replaceAll("\\s+", "");
	}

	private static String nombreBeneficiario(PagoProgramado pago) {
		if (pago.getTitular() != null) {
			return nvl(pago.getTitular().getNombre());
		}
		return nvl(pago.getBeneficiarioNombre());
	}

	private static String nvl(String valor) {
		return (valor != null) ? valor.trim() : "";
	}
}
