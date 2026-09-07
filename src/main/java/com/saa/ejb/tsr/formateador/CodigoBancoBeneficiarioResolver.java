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
 * ⚠ PENDIENTE DE CONFIRMAR: hoy devuelve {@code BEXTCDGO} (la PK de
 * {@code TSR.BEXT}) tal cual, asumiendo que ya es el código de institución
 * del BCE. Lo verifica
 * {@code docs/logica-negocio/tsr/sql/e2-15-verifica-si-bextcdgo-ya-es-el-codigo-bce.sql},
 * que el usuario todavía no corrió. Si el resultado dice que no lo es, esta
 * clase es el único lugar que hay que tocar.
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
		if (banco == null || banco.getCodigo() == null) {
			throw new IncomeException("No se pudo determinar el codigo de banco del beneficiario "
					+ "para el pago " + idPago + " (beneficiario " + nombreBeneficiario + "). "
					+ "Registre el banco del beneficiario antes de generar el archivo.");
		}
		return String.valueOf(banco.getCodigo());
	}
}
