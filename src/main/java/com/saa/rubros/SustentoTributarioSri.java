package com.saa.rubros;

/**
 * @author GaemiSoft
 * <p>Interfaz del rubro SustentoTributarioSri (238) - "sustento del comprobante" del Anexo
 * Transaccional Simplificado (ATS), Tabla 5 de la ficha técnica del SRI.</p>
 *
 * <p><b>El catálogo NO se redeclara aquí.</b> Ya está cargado en <code>PGS.LSRI</code>/
 * <code>PGS.TSRI</code> ({@link #LSRI_TABLA}, 15 códigos vigentes, <code>TSRI.ESTADO=1</code>):
 * es la fuente de verdad para validar un código y para listar descripciones -ver
 * <code>SustentoTributarioServiceImpl</code>, que consulta esa tabla en vez de un mapa Java.
 * Esta interfaz sólo trae, como constantes, los códigos que la lógica de este módulo referencia
 * directamente (los que dominan para esta empresa - ver
 * docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §3.6).</p>
 *
 * <p><b>Los códigos son cadenas de dos dígitos, no números.</b> El 00 y el cero a la izquierda
 * importan: el XSD del ATS (<code>codSustentoType</code>) exige <code>pattern="[0-9]{2}"</code>,
 * así que <code>"1"</code> no es un valor válido, tiene que ser <code>"01"</code>.</p>
 *
 * <p><b>Regla de resolución (corregida 2026-08-27):</b> la regla base mira el IVA de la
 * <i>factura</i> (<code>FCTC.VIVA</code>), no el grupo de producto — un mismo grupo mezcla
 * líneas con y sin IVA (ver docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §6.2). El
 * sustento por defecto del grupo (<code>GRPPCSUS</code>) sólo se consulta como
 * <b>excepción</b>, para los tres casos que el IVA no puede decidir por sí solo: activo fijo,
 * inventario y reembolso de gasto — ver {@link com.saa.ejb.cxp.service.SustentoTributarioService}.</p>
 */
public interface SustentoTributarioSri {

    /** PGS.LSRI.TABLA del catálogo "Cat ATS - T5 - Sustento del comprobante". */
    String LSRI_TABLA = "703";

    /** 01 - Crédito tributario para declaración de IVA (servicios y bienes distintos de inventarios y activos fijos). Regla base: factura con IVA (FCTC.VIVA > 0). */
    String CREDITO_TRIBUTARIO_IVA = "01";

    /** 02 - Costo o gasto para declaración de IR (servicios y bienes distintos de inventarios y activos fijos). Regla base: factura sin IVA (FCTC.VIVA = 0 o null). */
    String COSTO_GASTO_IR = "02";

    /** 03 - Activo fijo: crédito tributario para declaración de IVA. Excepción por grupo de producto (GRPPCSUS), nunca regla base. */
    String ACTIVO_FIJO_CREDITO_IVA = "03";

    /** 04 - Activo fijo: costo o gasto para declaración de IR. Excepción por grupo de producto (GRPPCSUS), nunca regla base. */
    String ACTIVO_FIJO_COSTO_GASTO_IR = "04";

    /** 06 - Inventario: crédito tributario para declaración de IVA. Excepción por grupo de producto (GRPPCSUS), nunca regla base. */
    String INVENTARIO_CREDITO_IVA = "06";

    /** 07 - Inventario: costo o gasto para declaración de IR. Excepción por grupo de producto (GRPPCSUS), nunca regla base. */
    String INVENTARIO_COSTO_GASTO_IR = "07";

    /** 08 - Valor pagado para solicitar reembolso de gasto (intermediario). Excepción por grupo de producto (GRPPCSUS), nunca regla base. */
    String REEMBOLSO_GASTO_INTERMEDIARIO = "08";

}
