package com.saa.ejb.cnt.flujo;

/**
 * Consultas nativas del flujo de caja (C38), solo lectura. Parametros posicionales ?1.. como en el resto del SAA; el
 * arnes de prueba fuera del servidor usa este mismo texto. Criterio de asientos: el del B17 (TempReportesDaoServiceImpl.selectSaldosPorCuenta:
 * empresa, estados ACTIVO y REVERSADO, por ASNTFCHA).
 */
public final class ConsultasFlujo {

	/**
	 * Neto (haber - debe) por asiento y cuenta del plan. ?1 empresa · ?2 ?3 estados del asiento · ?4 desde (incluida) ·
	 * ?5 hasta (excluida).
	 */
	public static final String LINEAS_POR_ASIENTO = "SELECT d.ASNTCDGO, p.PLNNCNTA, NVL(SUM(d.DTASHBRR), 0) - NVL(SUM(d.DTASDBEE), 0)"
			+ "  FROM CNT.DTAS d"
			+ "  JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO"
			+ "  JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO"
			+ " WHERE a.PJRQCDGO = ?1 AND a.ASNTESTD IN (?2, ?3)"
			+ "   AND a.ASNTFCHA >= ?4 AND a.ASNTFCHA < ?5"
			+ " GROUP BY d.ASNTCDGO, p.PLNNCNTA ORDER BY d.ASNTCDGO, p.PLNNCNTA";

	/** Fecha del primer asiento de la empresa (el arranque de la contabilidad en el SAA). ?1 empresa · ?2 ?3 estados. */
	public static final String PRIMER_ASIENTO = "SELECT MIN(a.ASNTFCHA) FROM CNT.ASNT a WHERE a.PJRQCDGO = ?1 AND a.ASNTESTD IN (?2, ?3)";

	private ConsultasFlujo() {
	}
}
