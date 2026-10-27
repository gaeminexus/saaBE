package com.saa.rubros;

/**
 * Estado de una corrida de acreditacion de vacaciones (RHH.CRVC.CRVCESTD). No es un catalogo
 * Rubro/DetalleRubro: es estado tecnico de proceso.
 */
public interface RhhEstadoCorridaAcreditacion {

	/** La corrida acredito y quedo confirmada. */
	String OK = "OK";

	/** La corrida fallo; el motivo esta en CRVCMNSJ. Se graba aparte, no se pierde con el rollback. */
	String ERROR = "ERROR";

	/**
	 * La corrida automatica NO acredito a proposito y lo dejo asentado: el anio esta en modalidad
	 * por aniversario, o ya hay una corrida OK del mismo anio con corte posterior. No cuenta como
	 * «ultima corrida OK» para decidir si la acreditacion esta desactualizada.
	 */
	String OMITIDA = "OMITIDA";

}
