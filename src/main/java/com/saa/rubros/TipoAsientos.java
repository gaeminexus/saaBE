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

	// Asiento de CXC - Anticipos de Clientes (codigoAlterno=4, TINGRESO)
	public static final int ANTICIPOS_CLIENTE = 4;

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

	/** Retenciones electrónicas v2 emitidas (CXC). codigoAlterno=3 en BD. */
	public static final int RETENCIONES_EMITIDAS_V2 = 3;

	// ─── CXP: Documentos de Compra (recibidos del proveedor vía SRI) ─────────
	// Los cinco comprobantes de la carga CXP —factura, NC, ND, liquidación y las
	// dos retenciones— comparten codigoAlterno=3. Decisión del usuario del
	// 2026-08-23; ya no queda nada por confirmar en BD para este bloque.
	// AuxiliarUno habitual:
	//   · Facturas de compra       → código del grupo de producto + código del proveedor
	//   · Notas de Crédito/Débito  → código del grupo de producto + código del proveedor
	//   · Liquidaciones de compra  → código del grupo de producto + código del proveedor/prestador
	//   · Retenciones recibidas    → código de la cuenta de retención (por código SRI)

	/** Anticipos entregados a proveedores (CXP). codigoAlterno=5 (TEGRESO). */
	public static final int ANTICIPOS_PROVEEDOR     = 5;

	/** Facturas de compra recibidas (CXP). codigoAlterno=3 en BD. */
	public static final int FACTURAS_COMPRA         = 3;

	/**
	 * Notas de Crédito de compra recibidas (CXP). codigoAlterno=3, el mismo de la
	 * factura de compra — decisión del usuario del 2026-08-23: <b>todos</b> los
	 * comprobantes de la carga CXP comparten el tipo de asiento de la factura de
	 * compra, igual que ya hacían las dos retenciones. No se crean tipos propios.
	 */
	public static final int NOTAS_CREDITO_COMPRA    = 3;

	/**
	 * Notas de Débito de compra recibidas (CXP). codigoAlterno=3, el mismo de la
	 * factura de compra — decisión del usuario del 2026-08-23, ver
	 * {@link #NOTAS_CREDITO_COMPRA}.
	 */
	public static final int NOTAS_DEBITO_COMPRA     = 3;

	/**
	 * Liquidaciones de compra recibidas (CXP). codigoAlterno=3, el mismo de la
	 * factura de compra — decisión del usuario del 2026-08-23, ver
	 * {@link #NOTAS_CREDITO_COMPRA}.
	 *
	 * <p><b>No confundir</b> con {@link #LIQUIDACIONES_COMPRA_EMITIDAS}, que es la
	 * de CXC: otra constante, otro flujo, y esa sigue con su codigoAlterno por
	 * definir.</p>
	 */
	public static final int LIQUIDACIONES_COMPRA_RECIBIDAS = 3;

	/** Retenciones v1 recibidas de proveedor (CXP). codigoAlterno=3 en BD. */
	public static final int RETENCIONES_RECIBIDAS   = 3;

	/** Retenciones v2 recibidas de proveedor (CXP). codigoAlterno=3 en BD. */
	public static final int RETENCIONES_RECIBIDAS_V2 = 3;

	// ─── Tesorería: aplicación de pagos y cobros a facturas ──────────────────
	// Salidas de dinero a proveedores → codigoAlterno 5 (TEGRESO), el mismo que
	// los anticipos a proveedores. Entradas de dinero de clientes → codigoAlterno
	// 4 (TINGRESO), el mismo que los anticipos de clientes.

	/** Cruce de anticipo de proveedor contra factura de compra (CXP). codigoAlterno=5 (TEGRESO).
	 *  DEBE: cuenta CxP del proveedor (PRCC tipo 1) / HABER: cuenta de anticipos (PRCC tipo 2). */
	public static final int APLICACION_ANTICIPO_PROVEEDOR = 5;

	/** Cruce de anticipo de cliente contra factura de venta (CXC). codigoAlterno=4 (TINGRESO).
	 *  DEBE: cuenta de anticipos (PRCC tipo 2) / HABER: cuenta CxC del cliente (PRCC tipo 1). */
	public static final int APLICACION_ANTICIPO_CLIENTE = 4;

	/** Pago a proveedor por transferencia bancaria (CXP). codigoAlterno=5 (TEGRESO).
	 *  DEBE: cuenta CxP del proveedor (PRCC tipo 1) / HABER: cuenta contable del banco. */
	public static final int PAGO_TRANSFERENCIA_CXP = 5;

	/** Cobro de cliente por transferencia bancaria (CXC). codigoAlterno=4 (TINGRESO).
	 *  DEBE: cuenta contable del banco / HABER: cuenta CxC del cliente (PRCC tipo 1). */
	public static final int COBRO_TRANSFERENCIA_CXC = 4;

	// ─── Tesorería: ingresos y egresos sin documento físico (TSR.INGR/EGRS) ──

	/** Egreso de tesorería sin documento (comisiones, administración de cuenta). codigoAlterno=5 (TEGRESO).
	 *  DEBE: cuenta del grupo del producto CXP / HABER: cuenta contable del banco. */
	public static final int EGRESO_TESORERIA = 5;

	/** Ingreso de tesorería sin documento (intereses ganados, créditos bancarios). codigoAlterno=4 (TINGRESO).
	 *  DEBE: cuenta contable del banco / HABER: cuenta del grupo del producto CXC. */
	public static final int INGRESO_TESORERIA = 4;

	// ─── CXP: pagos cuyo documento de origen vive en otro módulo ─────────────

	/**
	 * Pago de un documento originado FUERA de CXP (PGS.PGTR.PGTRORGN no nulo).
	 * codigoAlterno=5 (TEGRESO), el mismo que ya usan {@link #EGRESO_TESORERIA},
	 * {@link #ANTICIPOS_PROVEEDOR} y {@link #PAGO_TRANSFERENCIA_CXP}: no hace falta
	 * una fila nueva de TipoAsiento en BD.
	 * <p>
	 * DEBE: una línea por cada detalle de PGS.DPGT, con la cuenta del grupo de su
	 * producto de pago / HABER: cuenta contable del banco, por el total.
	 * <p>
	 * El asiento lo genera CXP, así que se etiqueta {@code ModuloSistema.CUENTAS_POR_PAGAR}.
	 * No existe un ModuloSistema por cada módulo que pueda originar el pago.
	 */
	public static final int PAGO_ORIGEN_EXTERNO = 5;

	// ─── RRHH: nómina ────────────────────────────────────────────────────────

	/**
	 * Asientos generados por el módulo de RRHH: rol de pagos, provisiones, pago y liquidación.
	 * codigoAlterno=6, asignado por el cliente el 2026-08-19. Los cuatro asientos comparten
	 * tipo; lo que los distingue es la plantilla (CNT.PLNS códigos alternos 163 a 166, leídos
	 * de RHH.CFNM: CFNMPLRL, CFNMPLPR, CFNMPLPG y CFNMPLLQ).
	 *
	 * <p><b>No confundir</b> con {@link ModuloSistema#RECURSOS_HUMANOS}, que vale 5 y es la
	 * etiqueta de módulo (último argumento de {@code generarAsiento}); este es el tipo de
	 * asiento contable (segundo argumento).
	 */
	public static final int RECURSOS_HUMANOS = 6;

}
