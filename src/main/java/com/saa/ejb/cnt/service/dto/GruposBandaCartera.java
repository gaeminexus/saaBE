package com.saa.ejb.cnt.service.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Agrupación Q/P/H de las cuentas de banda de la cartera, derivada de la configuración de bandas
 * ({@code CRD.BNDP → CRD.CBPR → CRD.PRDC.TPPRCDGO}) y no de los rangos 01-04/05-08/09-12 del código de cuenta
 * (P4-C, PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md). La arma una sola vez por cierre
 * {@code ParametroContableService#gruposBandaCartera} y la comparten el ajuste de bandas ⑨ del cierre de cartera y
 * los controles de cierre, para que no puedan divergir.
 *
 * <p>Una cuenta que no está en una banda activa de la empresa, o que sirve a productos de tipos de préstamo
 * distintos (o de un tipo sin letra), NO tiene grupo: {@link #grupoDe} devuelve {@link #SIN_GRUPO}. Nunca se
 * agrupa en silencio.</p>
 */
public class GruposBandaCartera {

	/** Grupo de una cuenta que no se puede derivar (fuera de BNDP, tipos mixtos o tipo sin letra). */
	public static final String SIN_GRUPO = "?";

	private final Map<String, String> grupoPorCuenta = new HashMap<String, String>();
	private final Map<String, String> nombrePorGrupo = new HashMap<String, String>();

	public void asigna(String cuenta, String grupo) {
		grupoPorCuenta.put(cuenta, grupo);
	}

	public void nombra(String grupo, String nombre) {
		nombrePorGrupo.put(grupo, nombre);
	}

	/** Letra del grupo de la cuenta (Q, P o H), o {@link #SIN_GRUPO} si no se puede derivar. */
	public String grupoDe(String cuenta) {
		String g = grupoPorCuenta.get(cuenta);
		return g != null ? g : SIN_GRUPO;
	}

	/** Rótulo del grupo: nombre del tipo de préstamo y cuenta mínima y máxima leídas; la letra si no hay. */
	public String nombreDe(String grupo) {
		String n = nombrePorGrupo.get(grupo);
		return n != null ? n : grupo;
	}
}
