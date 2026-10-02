package com.saa.rubros;

/**
 * Estado de una liquidacion de ex-colaborador (RHH.LQEX.LQEXESTD). No es un catalogo
 * Rubro/DetalleRubro -- decision D5 de
 * docs/logica-negocio/rhh/API-LIQUIDACION-EXCOLABORADORES.md §5.2, mismo criterio que
 * {@link EstadoUsuarioApp}.
 *
 * <p>Circuito (§4): REGISTRADA --enviarATesoreria--&gt; EN_TESORERIA --(Tesoreria confirma,
 * sincronizarPago)--&gt; PAGADA. Desde REGISTRADA o desde EN_TESORERIA (si el pago sigue
 * POR_APROBAR) se puede pasar a ANULADA.</p>
 */
public interface RhhEstadoLiquidacionExterna {

	int REGISTRADA = 1;
	int EN_TESORERIA = 2;
	int PAGADA = 3;
	int ANULADA = 4;

}
