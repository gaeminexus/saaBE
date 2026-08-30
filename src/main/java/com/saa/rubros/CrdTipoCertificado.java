package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro CrdTipoCertificado (244), tipo de certificado de partícipe
 *         (CRD.CRTF.CRTFTPCR). Cada tipo tiene su propia plantilla en rep/crd/RPRT_CRTF_*.
 *         Las condiciones de habilitación por tipo se definen después: por ahora el
 *         catálogo solo identifica la plantilla.
 */
public interface CrdTipoCertificado {

	/** Al día en sus obligaciones — RPRT_CRTF_ALDI */
	int AL_DIA_EN_OBLIGACIONES = 1;
	/** Haber recibido aportes (liquidación de la cuenta patronal) — RPRT_CRTF_APRT */
	int HABER_RECIBIDO_APORTES = 2;
	/** No adeudar: un crédito elegido, cancelado — RPRT_CRTF_NOAD */
	int NO_ADEUDAR_CREDITO = 3;
	/** No adeudar: global, todos sus créditos cancelados — RPRT_CRTF_NOAG */
	int NO_ADEUDAR_GLOBAL = 4;
	/** Licitud de fondos depositados — RPRT_CRTF_LCTD */
	int LICITUD_DE_FONDOS = 5;
	/** Recibió aportes patronales y no recibe jubilación mensual — RPRT_CRTF_PTRN */
	int APORTES_PATRONALES_SIN_JUBILACION = 6;

}
