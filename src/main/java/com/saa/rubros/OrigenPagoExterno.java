package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Etiquetas de los procesos externos que pueden originar un pago en CXP
 *         (PGS.PGTR.PGTRORGN).
 *
 *         Para CXP el valor es una CADENA OPACA: la guarda junto al id del documento
 *         origen (PGTRIDOR) y la devuelve en los listados, pero nunca la resuelve ni la
 *         interpreta. Ninguna clase de CXP importa nada del modulo que la produce.
 *
 *         Por eso esta interfaz vive en com.saa.rubros y no en com.saa.ejb.cxp: las
 *         constantes nombran modulos, pero solo como texto. Si manana se retira el modulo
 *         crd del producto comercial, se borra la constante correspondiente y NADA deja de
 *         compilar; las columnas PGTRORGN / PGTRIDOR simplemente quedan siempre nulas.
 */
public interface OrigenPagoExterno {

	/**
	 * Devolucion de aportes a un participe, originada en CRD.DVAP.
	 * PGTRIDOR lleva el CRD.DVAP.DVAPCDGO correspondiente.
	 */
	public static final String CRD_DEVOLUCION_APORTE = "CRD_DEVOLUCION_APORTE";

	/**
	 * Apertura o reposicion de una caja chica pagada desde una cuenta bancaria,
	 * originada en TSR.MVCH. PGTRIDOR lleva el TSR.MVCH.MVCHCDGO correspondiente.
	 */
	public static final String TSR_CAJA_CHICA = "TSR_CAJA_CHICA";

	/**
	 * Anticipo de sueldo entregado a un colaborador, originado en RHH.ANTE.
	 * PGTRIDOR lleva el RHH.ANTE.ANTECDGO correspondiente.
	 */
	public static final String RHH_ANTICIPO_EMPLEADO = "RHH_ANTICIPO_EMPLEADO";

	/**
	 * Devolucion de saldo a favor de un cliente, originada en CXC.AnticipoCliente.
	 * PGTRIDOR lleva el CXC.AnticipoCliente.id correspondiente.
	 */
	public static final String CXC_DEVOLUCION_CLIENTE = "CXC_DEVOLUCION_CLIENTE";

	/**
	 * Pago mensual de una pension complementaria a un jubilado, originado en CRD.PGPC
	 * (item 4 de jubilados, 2026-08-31). PGTRIDOR lleva el CRD.PGPC.PGPCCDGO correspondiente.
	 */
	public static final String CRD_PAGO_PENSION_COMPLEMENTARIA = "CRD_PAGO_PENSION_COMPLEMENTARIA";

	/**
	 * Pago consolidado de una orden de nomina, originado en RHH.RDPG.
	 * PGTRIDOR lleva el RHH.RDPG.RDPGCDGO correspondiente. A diferencia de los demas
	 * origenes de esta interfaz, RDPG no tiene columna de enlace al pago: el vinculo se
	 * resuelve consultando (origen, idOrigen) contra PGS.PGTR, para no acoplar el
	 * despliegue del frente 2 a un DDL nuevo (ver
	 * docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #4.2).
	 */
	public static final String RHH_NOMINA = "RHH_NOMINA";

}
