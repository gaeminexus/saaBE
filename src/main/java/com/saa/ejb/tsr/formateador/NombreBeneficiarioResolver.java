package com.saa.ejb.tsr.formateador;

import com.saa.model.cxp.PagoProgramado;
import com.saa.model.tsr.CuentaBancariaTitular;
import com.saa.model.tsr.Titular;

/**
 * Resuelve el nombre del beneficiario que va al archivo de pagos, detrás de un único método --
 * mismo motivo que {@link IdentificacionBeneficiarioResolver} y
 * {@link CodigoBancoBeneficiarioResolver}: que la especificación bancaria cambie una vez y no en
 * los dos formateadores. Ver docs/logica-negocio/tsr/API-IDENTIFICACION-CUENTA-BANCARIA.md §6.2.
 *
 * <p>Regla, en este orden:
 * <ol>
 *   <li>{@code pago.getCuentaDestino().getNombreTitularCuenta()} si no está en blanco -- la
 *       cuenta puede estar a nombre de otra persona, y el banco valida el nombre contra el
 *       número de cuenta.</li>
 *   <li>Si no: {@code pago.getTitular().getNombre()} -- lo de hoy.</li>
 *   <li>Sin titular (beneficiario ocasional): {@code pago.getBeneficiarioNombre()} (PGTRBFNM) --
 *       lo de hoy, no cambia.</li>
 * </ol>
 *
 * <p>⚠️ Los mensajes de error de los formateadores siguen usando su propio
 * {@code nombreBeneficiario(pago)} (título/proveedor del pago), NO este resolver: quien lee un
 * error busca el pago por su proveedor, no por el dueño de la cuenta. No se unifican (§6.2).
 */
public final class NombreBeneficiarioResolver {

	private NombreBeneficiarioResolver() {
	}

	/**
	 * @param pago : Pago con {@code cuentaDestino} poblado (cuenta de un titular) o con
	 *               beneficiario ocasional (PGTRBFCT no vacío)
	 * @return     : Nombre del beneficiario a escribir en el archivo del banco
	 */
	public static String resolver(PagoProgramado pago) {
		CuentaBancariaTitular cuenta = pago.getCuentaDestino();
		if (cuenta != null && cuenta.getNombreTitularCuenta() != null
				&& !cuenta.getNombreTitularCuenta().trim().isEmpty()) {
			return cuenta.getNombreTitularCuenta().trim();
		}

		Titular titular = pago.getTitular();
		if (titular != null) {
			return nvl(titular.getNombre());
		}

		return nvl(pago.getBeneficiarioNombre());
	}

	private static String nvl(String valor) {
		return (valor != null) ? valor.trim() : "";
	}
}
