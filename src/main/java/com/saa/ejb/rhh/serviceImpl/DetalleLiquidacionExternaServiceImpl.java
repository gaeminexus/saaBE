package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.DetalleLiquidacionExternaDaoService;
import com.saa.ejb.rhh.service.DetalleLiquidacionExternaService;
import com.saa.model.rhh.DetalleLiquidacionExterna;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz DetalleLiquidacionExternaService.
 *  Contiene los servicios relacionados con la entidad DetalleLiquidacionExterna.</p>
 */
@Stateless
public class DetalleLiquidacionExternaServiceImpl implements DetalleLiquidacionExternaService {

	@EJB
	private DetalleLiquidacionExternaDaoService detalleLiquidacionExternaDaoService;

	@Override
	public void save(List<DetalleLiquidacionExterna> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleLiquidacionExterna service");
		for (DetalleLiquidacionExterna registro : lista) {
			detalleLiquidacionExternaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de detalleLiquidacionExterna service");
		DetalleLiquidacionExterna detalleLiquidacionExterna = new DetalleLiquidacionExterna();
		for (Long registro : id) {
			detalleLiquidacionExternaDaoService.remove(detalleLiquidacionExterna, registro);
		}
	}

	@Override
	public List<DetalleLiquidacionExterna> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DetalleLiquidacionExterna");
		List<DetalleLiquidacionExterna> result =
				detalleLiquidacionExternaDaoService.selectAll(NombreEntidadesRhh.DETALLE_LIQUIDACION_EXTERNA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de detalleLiquidacionExterna no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public DetalleLiquidacionExterna selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de detalleLiquidacionExterna con id: " + id);
		return detalleLiquidacionExternaDaoService.selectById(id, NombreEntidadesRhh.DETALLE_LIQUIDACION_EXTERNA);
	}

	@Override
	public List<DetalleLiquidacionExterna> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleLiquidacionExterna");
		List<DetalleLiquidacionExterna> result = detalleLiquidacionExternaDaoService
				.selectByCriteria(datos, NombreEntidadesRhh.DETALLE_LIQUIDACION_EXTERNA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de detalleLiquidacionExterna no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public DetalleLiquidacionExterna saveSingle(DetalleLiquidacionExterna detalleLiquidacionExterna) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) DetalleLiquidacionExterna");
		return detalleLiquidacionExternaDaoService.save(detalleLiquidacionExterna, detalleLiquidacionExterna.getCodigo());
	}
}
