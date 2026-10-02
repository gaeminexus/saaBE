package com.saa.ejb.rhh.service.dto;

import java.util.List;

import com.saa.model.rhh.DetalleLiquidacionExterna;
import com.saa.model.rhh.LiquidacionExterna;

/**
 * Cuerpo de {@code POST /lqex/registrar} y {@code PUT /lqex/actualizar}: la cabecera y sus
 * conceptos en un solo mensaje (§6 de
 * docs/logica-negocio/rhh/API-LIQUIDACION-EXCOLABORADORES.md).
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok -- mismo criterio que
 * {@code com.saa.ejb.cxp.service.dto}.
 */
public class SolicitudLiquidacionExterna {

	/** Cabecera de la liquidacion. */
	private LiquidacionExterna liquidacion;

	/** Conceptos de la liquidacion. */
	private List<DetalleLiquidacionExterna> detalles;

	public SolicitudLiquidacionExterna() {
	}

	public LiquidacionExterna getLiquidacion() {
		return liquidacion;
	}

	public void setLiquidacion(LiquidacionExterna liquidacion) {
		this.liquidacion = liquidacion;
	}

	public List<DetalleLiquidacionExterna> getDetalles() {
		return detalles;
	}

	public void setDetalles(List<DetalleLiquidacionExterna> detalles) {
		this.detalles = detalles;
	}
}
