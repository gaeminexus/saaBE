package com.saa.ejb.cnt.service;

import java.util.List;
import java.util.Map;

import jakarta.ejb.Local;

/**
 * Lectura de los parámetros contables del rubro {@link com.saa.rubros.Rubros#CNT_PARAMETROS_ESTADOS_FINANCIEROS}
 * (250), detalles en {@link com.saa.rubros.CntParametroEstadoFinanciero}. Único lugar que los consulta fuera de los
 * estados financieros oficiales: los controles de cierre (cnt) y el cierre de cartera (crd) leen de acá las cuentas de
 * nivel 1-2 que antes estaban escritas en el código (P2, P8 y P4-A de PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md).
 *
 * <p>Sin respaldo: si el detalle no existe, está inactivo o vacío, {@link #valor(int)} devuelve {@code null} y el
 * llamador decide (el cierre se detiene, el control avisa «No se pudo medir»).</p>
 */
@Local
public interface ParametroContableService {

	/** Detalles activos y no vacíos del rubro 250: PDTRALTR → PDTRVLRV (ya recortado). */
	Map<Integer, String> parametrosEstadosFinancieros();

	/** Valor del detalle {@code alternoDetalle} del rubro 250, o {@code null} si falta, está inactivo o vacío. */
	String valor(int alternoDetalle);

	/**
	 * P4-B (2026-11-01): cuentas contables de la PROVISIÓN de cartera de la empresa = el conjunto distinto de las
	 * cuentas de las líneas {@link com.saa.rubros.CrdLineaAsiento#PROVISION_CARTERA} (aux1 83, una por tipo de
	 * préstamo) de la plantilla {@link com.saa.rubros.PlantillasCredito#PROVISION_CARTERA} (alterno 37). Lista VACÍA
	 * si falta la plantilla o la línea: el llamador decide (el cierre se detiene, los controles avisan «No se pudo
	 * medir»). No hay cuentas de respaldo.
	 */
	List<String> cuentasProvisionCartera(Long idEmpresa) throws Throwable;

	/**
	 * P4-C (2026-11-01): agrupación Q/P/H de las cuentas de banda de la cartera derivada de la configuración de bandas
	 * activa de la empresa ({@code CRD.BNDP → CRD.CBPR → CRD.PRDC.TPPRCDGO}): el tipo de préstamo del producto da la
	 * letra (1 = Q, 3 = P, 2 = H, las constantes de {@link com.saa.rubros.CrdLineaAsiento}). Solo bandas activas con
	 * cuenta bajo {@code prefijoRaiz}. Una cuenta con tipos mixtos o con un tipo sin letra queda sin grupo.
	 */
	com.saa.ejb.cnt.service.dto.GruposBandaCartera gruposBandaCartera(Long idEmpresa, String prefijoRaiz);

	/**
	 * P3-ARRANQUE (2026-11-01): para cada madre ({@code 1.3.NN}, los primeros 6 caracteres de la cuenta) de la
	 * cartera, la cuenta de la banda de MAYOR {@code BNDPNMRO} de cada configuración de banda activa
	 * ({@code CRD.BNDP/CBPR}) de la empresa. Solo incluye las madres en que TODAS las configuraciones coinciden en
	 * una única cuenta; una madre sin bandas o con varias cuentas en la banda máxima no aparece (el llamador avisa,
	 * no adivina). Es la cuenta que absorbe la diferencia neta del ⑨ en el modo arranque.
	 */
	Map<String, String> cuentaBandaMayorPorMadre(Long idEmpresa, String prefijoRaiz);

	/** La cuenta como prefijo de sus subcuentas: la misma con un punto al final si no lo trae ({@code 1.3} → {@code 1.3.}). */
	static String comoPrefijo(String cuenta) {
		return cuenta.endsWith(".") ? cuenta : cuenta + ".";
	}
}
