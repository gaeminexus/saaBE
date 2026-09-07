package com.saa.rubros;

/**
 * Estado técnico de una credencial de la app móvil (CRD.USAP.USAPESTD). No es un catálogo
 * Rubro/DetalleRubro — es estado de cuenta, no parametría de negocio — mismo criterio que
 * {@link Estado}.
 */
public interface EstadoUsuarioApp {

	int ACTIVO = 1;
	int BLOQUEADO = 2;
	int ELIMINADO = 3;

}
