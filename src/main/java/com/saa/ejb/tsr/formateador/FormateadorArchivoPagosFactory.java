package com.saa.ejb.tsr.formateador;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.saa.model.tsr.Banco;
import com.saa.model.tsr.CuentaBancaria;

/**
 * Resuelve qué {@link FormateadorArchivoPagos} usar según el banco de la
 * CUENTA DE ORIGEN (la cuenta propia desde la que se paga) — nunca por el
 * banco del beneficiario. Mismo patrón que
 * {@link com.saa.ejb.tsr.parser.BankStatementParserFactory}, copiado tal
 * cual: mismo mapa de palabra clave → proveedor, misma normalización (sin
 * tildes, en mayúsculas) y el mismo fallo explícito cuando el banco no
 * tiene implementación. Ver
 * docs/logica-negocio/pagos/FORMATO-ARCHIVO-BANCOS.md §5.
 */
public final class FormateadorArchivoPagosFactory {

	private static final Map<String, Supplier<FormateadorArchivoPagos>> FORMATEADORES_POR_PALABRA_CLAVE = crearMapa();

	private FormateadorArchivoPagosFactory() {
	}

	private static Map<String, Supplier<FormateadorArchivoPagos>> crearMapa() {
		Map<String, Supplier<FormateadorArchivoPagos>> mapa = new LinkedHashMap<>();
		mapa.put("INTERNACIONAL", InternacionalArchivoPagoFormateador::new);
		mapa.put("PACIFICO", PacificoArchivoPagoFormateador::new);
		return mapa;
	}

	/**
	 * Resuelve el formateador a usar para la cuenta bancaria de origen indicada.
	 * @param cuenta : Cuenta bancaria de origen (con su Banco ya cargado)
	 * @return        : Nueva instancia del formateador correspondiente
	 * @throws IllegalArgumentException : Si no hay formateador registrado para el banco
	 */
	public static FormateadorArchivoPagos resolver(CuentaBancaria cuenta) {
		Banco banco = cuenta.getBanco();
		if (banco == null || banco.getNombre() == null || banco.getNombre().isBlank()) {
			throw new IllegalArgumentException(
				"La cuenta bancaria " + cuenta.getCodigo() + " no tiene un banco asociado con nombre valido");
		}
		String nombreNormalizado = normalizar(banco.getNombre());
		for (Map.Entry<String, Supplier<FormateadorArchivoPagos>> entrada : FORMATEADORES_POR_PALABRA_CLAVE.entrySet()) {
			if (nombreNormalizado.contains(entrada.getKey())) {
				return entrada.getValue().get();
			}
		}
		throw new IllegalArgumentException(
			"No hay formato de archivo de pagos implementado para el banco '" + banco.getNombre() + "'. "
				+ "Bancos soportados: " + FORMATEADORES_POR_PALABRA_CLAVE.keySet());
	}

	private static String normalizar(String texto) {
		String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return sinTildes.toUpperCase().trim();
	}
}
