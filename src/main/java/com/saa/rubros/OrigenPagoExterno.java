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
	 * Valor acortado a 26 caracteres (2026-09-04): "CRD_PAGO_PENSION_COMPLEMENTARIA" son 31 y
	 * PGS.PGTR.PGTRORGN es VARCHAR2(30) -- ORA-12899 en todo INSERT. Ninguna fila pudo
	 * grabarse jamas con el valor viejo, asi que no hace falta migrar datos.
	 */
	public static final String CRD_PAGO_PENSION_COMPLEMENTARIA = "CRD_PENSION_COMPLEMENTARIA";

	/**
	 * Pago consolidado de una orden de nomina, originado en RHH.RDPG.
	 * PGTRIDOR lleva el RHH.RDPG.RDPGCDGO correspondiente. A diferencia de los demas
	 * origenes de esta interfaz, RDPG no tiene columna de enlace al pago: el vinculo se
	 * resuelve consultando (origen, idOrigen) contra PGS.PGTR, para no acoplar el
	 * despliegue del frente 2 a un DDL nuevo (ver
	 * docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #4.2).
	 */
	public static final String RHH_NOMINA = "RHH_NOMINA";

	/**
	 * Pago consolidado de una orden de beneficio social (decimo acumulado, fondos de
	 * reserva), originado en RHH.ODBS. PGTRIDOR lleva el RHH.ODBS.ODBSCDGO correspondiente.
	 * Uno solo para los tres tipos de beneficio: el tipo concreto ya viaja en ODBSTPBN, y la
	 * bandeja gana un filtro legible en vez de tres origenes casi iguales (ver
	 * docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #3.1).
	 */
	public static final String RHH_BENEFICIO_SOCIAL = "RHH_BENEFICIO_SOCIAL";

	/**
	 * Desembolso de un prestamo al aprobarlo, originado en CRD.PRST. PGTRIDOR lleva el
	 * CRD.PRST.PRSTCDGO correspondiente. Nace sin cuenta de origen (idCuentaBancariaOrigen
	 * null): tesoreria asigna cuenta y forma de pago al aprobar, igual que
	 * CRD_DEVOLUCION_APORTE. Ver docs/logica-negocio/crd/PLAN-DESEMBOLSO-PRESTAMO.md.
	 */
	public static final String CRD_DESEMBOLSO_PRESTAMO = "CRD_DESEMBOLSO_PRESTAMO";

}
