package com.saa.ejb.cxc.util;

/**
 * ÍTEM 10 (docs/logica-negocio/sri/PLAN-ATS-AJUSTES-2026-09-15.md §1), encargo 2026-09-15.
 * <p>
 * El SRI exige que {@code numDocSustento} (retención) y el número de una compra
 * ({@code establecimiento-puntoEmision-secuencial}) lleguen en 15 dígitos, {@code EEE-PPP-SSSSSSSSS}
 * sin guiones. Una factura electrónica siempre trae los tres segmentos completos porque salen del
 * XML del SRI; una nota de venta preimpresa se tipea a mano y puede llegar con un segmento corto
 * (docs/logica-negocio/cxc/PLAN-RETENCION-SOBRE-NOTA-DE-VENTA.md §6, retención 278 devuelta por un
 * secuencial de 7 dígitos).
 * <p>
 * Antes de este ítem la regla vivía DUPLICADA: sólo en
 * {@code RetencionV2ServiceImpl.normalizarNumDocSustento} (commit {@code 1b44e51c}), usada del lado
 * de la retención. El ÍTEM 11 del mismo plan necesita la MISMA regla del lado de la compra
 * ({@code GeneradorAtsServiceImpl}), para poder enlazar retención y compra por
 * {@code autorización + número} en vez de sólo autorización -- que en una nota de venta preimpresa
 * la comparten varias del mismo talonario. Dos copias de esta regla es exactamente el defecto que
 * el registro de reservas del equipo (§24) advierte: una diverge de la otra y nadie se entera hasta
 * que un caso real las distingue.
 */
public final class NumeroDocumentoSri {

	/**
	 * Si {@code numDocumento} tiene forma {@code E-P-S} con los tres segmentos sólo de dígitos y
	 * dentro de 3/3/9, completa cada uno con ceros a la izquierda y los une sin guiones (15
	 * dígitos). Cualquier otra forma se deja igual que antes -- sólo quita guiones, sin adivinar.
	 * Cuerpo idéntico al que tenía {@code RetencionV2ServiceImpl.normalizarNumDocSustento}
	 * (commit {@code 1b44e51c}); el único cambio es inline del {@code nvl} privado de esa clase,
	 * que no existe acá.
	 */
	public static String normalizarA15(String numDocumento) {
		String valor = numDocumento != null ? numDocumento : "";
		String[] partes = valor.split("-");
		if (partes.length == 3
				&& partes[0].matches("[0-9]{1,3}")
				&& partes[1].matches("[0-9]{1,3}")
				&& partes[2].matches("[0-9]{1,9}")) {
			return "0".repeat(3 - partes[0].length()) + partes[0]
					+ "0".repeat(3 - partes[1].length()) + partes[1]
					+ "0".repeat(9 - partes[2].length()) + partes[2];
		}
		return valor.replace("-", "");
	}

	/** Arma {@code "establecimiento-puntoEmision-secuencial"} y aplica {@link #normalizarA15(String)}. */
	public static String normalizarA15(String establecimiento, String puntoEmision, String secuencial) {
		return normalizarA15(establecimiento + "-" + puntoEmision + "-" + secuencial);
	}

	private NumeroDocumentoSri() {
	}
}
