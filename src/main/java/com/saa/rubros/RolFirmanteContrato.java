package com.saa.rubros;

/**
 * Rol de un firmante del contrato de adhesión ({@code CRD.FRFR.FRFRROLL}). El SAA envía SOLO al partícipe; la firma del
 * representante legal de ASOPREP la pone Security Data en su plataforma (D1), por eso no hay un rol para él.
 */
public interface RolFirmanteContrato {
	public static final String PARTICIPE = "PARTICIPE";
}
