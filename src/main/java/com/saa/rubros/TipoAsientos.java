/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro TipoAsientos (45)
 */
public interface TipoAsientos {

	// Ids de los elementos hijos
	public static final int RAIZ = 0;
	public static final int ASIENTO_CIERRE = 1;
	public static final int INGRESOS = 2;
	public static final int EGRESOS = 3;
	public static final int DEBITO_BANCARIO = 4;
	public static final int CREDITO_BANCARIO = 5;
	public static final int DEPOSITO_BANCARIO = 6;
	public static final int TRANSFERENCIAS = 7;

	// Asiento de CXC - Facturas de Venta (codigoAlterno=2, sistema=1)
	public static final int FACTURAS_VENTA = 2;

	// Asiento de CXC - Anticipos de Clientes (codigoAlterno=8, sistema=1)
	public static final int ANTICIPOS_CLIENTE = 8;

	// ─── CXC: Documentos de Cobro ─────────────────────────────────────────────
	// TODO: Confirmar codigoAlterno real de la plantilla en BD para cada tipo.
	// AuxiliarUno habitual:
	//   · Notas de Crédito / Débito → código del grupo de producto + código del cliente
	//   · Liquidaciones de Compra   → código del grupo de producto + código del proveedor
	//   · Retenciones emitidas      → código de la cuenta de retención (por código SRI)

	/** Notas de Crédito emitidas por la empresa (ventas). Pendiente definir codigoAlterno. */
	public static final int NOTAS_CREDITO_VENTA   = 3;  // TODO: verificar codigoAlterno en BD

	/** Notas de Débito emitidas por la empresa (ventas). Pendiente definir codigoAlterno. */
	public static final int NOTAS_DEBITO_VENTA     = 4;  // TODO: verificar codigoAlterno en BD

	/** Liquidaciones de compra emitidas por la empresa. Pendiente definir codigoAlterno. */
	public static final int LIQUIDACIONES_COMPRA_EMITIDAS = 5;  // TODO: verificar codigoAlterno en BD

	/** Retenciones electrónicas v1 emitidas (CXC). Pendiente definir codigoAlterno. */
	public static final int RETENCIONES_EMITIDAS   = 6;  // TODO: verificar codigoAlterno en BD

	/** Retenciones electrónicas v2 emitidas (CXC). Pendiente definir codigoAlterno. */
	public static final int RETENCIONES_EMITIDAS_V2 = 7; // TODO: verificar codigoAlterno en BD

	// ─── CXP: Documentos de Compra (recibidos del proveedor vía SRI) ─────────
	// TODO: Confirmar codigoAlterno real de la plantilla en BD para cada tipo.
	// AuxiliarUno habitual:
	//   · Facturas de compra       → código del grupo de producto + código del proveedor
	//   · Notas de Crédito/Débito  → código del grupo de producto + código del proveedor
	//   · Liquidaciones de compra  → código del grupo de producto + código del proveedor/prestador
	//   · Retenciones recibidas    → código de la cuenta de retención (por código SRI)

	/** Facturas de compra recibidas (CXP). Pendiente definir codigoAlterno. */
	public static final int FACTURAS_COMPRA         = 9;  // TODO: verificar codigoAlterno en BD

	/** Notas de Crédito de compra recibidas (CXP). Pendiente definir codigoAlterno. */
	public static final int NOTAS_CREDITO_COMPRA    = 10; // TODO: verificar codigoAlterno en BD

	/** Notas de Débito de compra recibidas (CXP). Pendiente definir codigoAlterno. */
	public static final int NOTAS_DEBITO_COMPRA     = 11; // TODO: verificar codigoAlterno en BD

	/** Liquidaciones de compra recibidas (CXP). Pendiente definir codigoAlterno. */
	public static final int LIQUIDACIONES_COMPRA_RECIBIDAS = 12; // TODO: verificar codigoAlterno en BD

	/** Retenciones v1 recibidas de proveedor (CXP). Pendiente definir codigoAlterno. */
	public static final int RETENCIONES_RECIBIDAS   = 13; // TODO: verificar codigoAlterno en BD

	/** Retenciones v2 recibidas de proveedor (CXP). Pendiente definir codigoAlterno. */
	public static final int RETENCIONES_RECIBIDAS_V2 = 14; // TODO: verificar codigoAlterno en BD

}
