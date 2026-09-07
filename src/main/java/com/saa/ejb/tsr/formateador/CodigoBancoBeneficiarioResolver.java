package com.saa.ejb.tsr.formateador;

import com.saa.basico.util.IncomeException;
import com.saa.model.tsr.BancoExterno;

/**
 * Resuelve el código de banco del beneficiario que exigen los dos formatos
 * nuevos (campo 12 del Internacional, columna B del Pacífico) detrás de un
 * único método, para que la respuesta del {@code e2-15} cambie una línea acá
 * y no dos formateadores. Ver
 * docs/logica-negocio/pagos/FORMATO-ARCHIVO-BANCOS.md §3.
 *
 * ✅ CONFIRMADO con el {@code e2-17} (2026-09-07): {@code BEXTTRJT} —el campo
 * {@code tarjeta} de {@link BancoExterno}, mapeado con nombre engañoso— ES el
 * código de institución financiera del BCE. 389 filas, 387 valores distintos
 * (mínimo 10, máximo 9997: no es un booleano), con las dos anclas del manual
 * del Pacífico verificadas (Banco de Machala → 25, Banco del Pacífico → 30,
 * sin que ningún otro banco repita esos valores) y confirmado además porque
 * la especificación del Internacional dice que el campo 12 vacío se
 * interpreta como 32 = Banco Internacional, y en la base {@code BEXTTRJT = 32}
 * es justamente {@code BANCO INTERNACIONAL}.
 */
public final class CodigoBancoBeneficiarioResolver {

	private CodigoBancoBeneficiarioResolver() {
	}

	/**
	 * @param banco              : Banco externo del beneficiario (TSR.BEXT)
	 * @param idPago             : Id del pago, para nombrarlo en el mensaje de error
	 * @param nombreBeneficiario : Nombre del beneficiario, para nombrarlo en el mensaje de error
	 * @return : Código de banco para el archivo, nunca vacío ni nulo
	 * @throws IncomeException : Si el banco o su código no están disponibles. No hay valor
	 *   por defecto posible: el Internacional interpreta el campo vacío como el código 32
	 *   (Banco Internacional), así que enviarlo vacío instruiría una transferencia al banco
	 *   equivocado en vez de rechazarla.
	 */
	public static String codigoBancoBeneficiario(BancoExterno banco, Long idPago, String nombreBeneficiario) {
		if (banco == null || banco.getTarjeta() == null) {
			throw new IncomeException("No se pudo determinar el codigo de banco del beneficiario "
					+ "para el pago " + idPago + " (beneficiario " + nombreBeneficiario + "). "
					+ "Registre el banco del beneficiario antes de generar el archivo.");
		}
		return String.valueOf(banco.getTarjeta());
	}
}
