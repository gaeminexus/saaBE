package com.saa.ejb.cxc.util;

/**
 * ÍTEM 17 (docs/logica-negocio/cxc/API-RUC-PROVEEDOR-SISTEMA-SRI.md), encargo 2026-09-10.
 * <p>
 * La <b>Resolución NAC-DGERCGC26-00000027</b> (vigente desde el 28-jul-2026), junto con el
 * <b>Anexo 26 de la Ficha Técnica v2.34</b>, obliga a que TODO comprobante electrónico lleve
 * dentro de {@code <infoAdicional>} un {@code <campoAdicional>} con el RUC del proveedor del
 * sistema de facturación electrónica que lo emitió.
 * <p>
 * ⏰ <b>Fecha límite: 26 de septiembre de 2026</b> (60 días calendario desde la publicación).
 * <p>
 * Una sola constante, no seis literales sueltos repartidos entre {@code FacturaServiceImpl},
 * {@code NotaCreditoServiceImpl}, {@code NotaDebitoServiceImpl},
 * {@code LiquidacionCompraServiceImpl}, {@code RetencionServiceImpl} y
 * {@code RetencionV2ServiceImpl} -- este repositorio lleva tres tropiezos registrados en
 * septiembre de 2026 por la misma causa (un valor repetido lejos de su fuente que se
 * desincroniza): los {@code COLUMN_n} de los {@code .jrxml} con {@code SELECT *}, el {@code 5}
 * de estado de emisión que se usó suelto antes de {@code CriterioVentaVigente}, y el
 * {@code "05"} de tipo de identificación antes del ítem 13. Seis copias de un RUC habría sido
 * la próxima.
 * <p>
 * El valor es una propiedad del <b>software</b> (GAEMII NEXUS S.A.S.), no del contribuyente: no
 * cambia por empresa ni por facturador, y por eso es una constante y no un parámetro de base.
 */
public final class ProveedorSistemaSri {

	/**
	 * RUC del proveedor del sistema de facturación electrónica (GAEMII NEXUS S.A.S.), exigido
	 * por la Resolución NAC-DGERCGC26-00000027. <b>No es el RUC del emisor ni del cliente.</b>
	 * Es el mismo para todos los facturadores y todas las empresas que usen el SAA.
	 */
	public static final String RUC_PROVEEDOR_SISTEMA = "1793228946001";

	/**
	 * Nombre EXACTO del {@code campoAdicional} que exige la norma -- mayúscula en las dos
	 * palabras, un espacio en el medio. El SRI compara el atributo {@code nombre} como texto,
	 * no como catálogo: no traducir, no cambiar mayúsculas, no tocar el espaciado.
	 */
	public static final String CAMPO_RUC_PROVEEDOR = "RUC Proveedor";

	private ProveedorSistemaSri() {
	}
}
