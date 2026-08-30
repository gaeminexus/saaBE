package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro CrdParametroCertificado (243), parámetros de los certificados
 *         de partícipe. Los valores viven en SCP.PDTR.PDTRVLRV y se leen con
 *         {@code DetalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.CRD_PARAMETROS_CERTIFICADOS, alterno)}.
 *         Cambiar la persona que firma es un UPDATE en la base, sin tocar los reportes.
 */
public interface CrdParametroCertificado {

	/** Nombre del firmante (ej. "Lic. Gabriel Patricio Robayo Rueda") */
	int FIRMANTE = 1;
	/** Cargo del firmante (ej. "Jefe de Crédito") */
	int CARGO_FIRMANTE = 2;
	/** Ciudad de emisión que encabeza la fecha (ej. "Quito") */
	int CIUDAD_EMISION = 3;

}
