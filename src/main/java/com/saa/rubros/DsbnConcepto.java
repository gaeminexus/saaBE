package com.saa.rubros;

/**
 * Concepto distribuido (CRD.DSBN.DSBNCNCP) — el agrupador PRIMARIO de la pantalla de
 * auditoría de bandas, ver PLAN-AUDITORIA-BANDAS.md §3. NO agrupar por cuenta contable: la
 * mora va a la misma cuenta que el interés ordinario y se fusionarían.
 *
 * Solo {@link #CAPITAL} lleva banda (tipo de cartera, días, banda, etiqueta); en los demás
 * conceptos esos campos vienen en null — ausencia de dato legítima.
 */
public interface DsbnConcepto {

	public static final String CAPITAL = "CAPITAL";
	public static final String INTERES_ORDINARIO = "INTERES_ORDINARIO";
	public static final String INTERES_MORA = "INTERES_MORA";
	public static final String INTERES_VENCIDO = "INTERES_VENCIDO";
	public static final String SEGURO_DESGRAVAMEN = "SEGURO_DESGRAVAMEN";
	public static final String SEGURO_INCENDIO = "SEGURO_INCENDIO";
	public static final String APORTE = "APORTE";

}
