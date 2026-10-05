package com.saa.rubros;

/**
 * Estado técnico de un detalle de orden de pago de nómina (RHH.DRPG.DRPGESTD). No es un
 * catálogo Rubro/DetalleRubro — es estado de cuenta, no parametría de negocio — mismo
 * criterio que {@link EstadoUsuarioApp}.
 *
 * <p>Hasta la "nómina por empleado" (docs/logica-negocio/rhh/API-PAGO-NOMINA-POR-EMPLEADO.md),
 * DRPGESTD solo tenía el valor 1 sin significado (sin CHECK en base, rhh/sql/04 línea 141:
 * siempre ACTIVO). Con el pago por empleado desde Tesorería pasa a representar el estado del
 * pago de esa línea frente al banco.</p>
 */
public interface RhhEstadoDetalleOrdenPago {

	/** Pago en tramite en la bandeja de tesoreria (valor por defecto de siempre). */
	int PENDIENTE = 1;

	/** El pago fue CONFIRMADO por el banco. */
	int PAGADO = 2;

	/** El pago fue RECHAZADO o ANULADO; DRPGRCHZ='S' y DRPGMTRC trae el motivo. */
	int RECHAZADO = 3;

}
