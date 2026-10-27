package com.saa.rubros;

/**
 * Origen de una corrida de acreditacion de vacaciones (RHH.CRVC.CRVCORGN). No es un catalogo
 * Rubro/DetalleRubro: es estado tecnico de proceso, mismo criterio que
 * {@link RhhModalidadVacaciones}.
 */
public interface RhhOrigenCorridaAcreditacion {

	/** La corrida la disparo una persona desde la pantalla «Acreditar vacaciones». */
	String MANUAL = "MANUAL";

	/** La corrida la disparo el cierre del periodo de nomina. */
	String CIERRE_NOMINA = "CIERRE_NOMINA";

}
