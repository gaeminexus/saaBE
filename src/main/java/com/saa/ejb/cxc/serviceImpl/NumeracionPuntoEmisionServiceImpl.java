package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.NumeracionPuntoEmisionDaoService;
import com.saa.ejb.cxc.service.NumeracionPuntoEmisionService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.NumeracionPuntoEmision;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio para la entidad NumeracionPuntoEmision
 */
@Stateless
public class NumeracionPuntoEmisionServiceImpl implements NumeracionPuntoEmisionService {
	
	@EJB
	private NumeracionPuntoEmisionDaoService numeracionDaoService;

	@Override
	public void save(List<NumeracionPuntoEmision> lista) throws Throwable {
		System.out.println("NumeracionPuntoEmisionServiceImpl.save - cantidad: " + lista.size());
		for (NumeracionPuntoEmision numeracion : lista) {
			numeracionDaoService.save(numeracion, numeracion.getId());
		}
	}

	@Override
	public void remove(List<Long> ids) throws Throwable {
		System.out.println("NumeracionPuntoEmisionServiceImpl.remove - cantidad: " + ids.size());
		NumeracionPuntoEmision numeracion = new NumeracionPuntoEmision();
		for (Long id : ids) {
			numeracionDaoService.remove(numeracion, id);
		}
	}

	@Override
	public List<NumeracionPuntoEmision> selectAll() throws Throwable {
		System.out.println("NumeracionPuntoEmisionServiceImpl.selectAll");
		List<NumeracionPuntoEmision> result = numeracionDaoService.selectAll(NombreEntidadesCobro.NUMERACION_PUNTO_EMISION);
		if (result.isEmpty()) {
			throw new IncomeException("Búsqueda completa de NumeracionPuntoEmision no devolvió ningún registro");
		}
		return result;
	}

	@Override
	public NumeracionPuntoEmision selectById(Long id) throws Throwable {
		System.out.println("NumeracionPuntoEmisionServiceImpl.selectById - id: " + id);
		return numeracionDaoService.selectById(id, NombreEntidadesCobro.NUMERACION_PUNTO_EMISION);
	}

	@Override
	public List<NumeracionPuntoEmision> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("NumeracionPuntoEmisionServiceImpl.selectByCriteria");
		List<NumeracionPuntoEmision> result = numeracionDaoService.selectByCriteria(datos, NombreEntidadesCobro.NUMERACION_PUNTO_EMISION);
		if (result.isEmpty()) {
			throw new IncomeException("Búsqueda por criterio de NumeracionPuntoEmision no devolvió ningún registro");
		}
		return result;
	}

	@Override
	public NumeracionPuntoEmision saveSingle(NumeracionPuntoEmision numeracion) throws Throwable {
		System.out.println("NumeracionPuntoEmisionServiceImpl.saveSingle");
		numeracion = numeracionDaoService.save(numeracion, numeracion.getId());
		return numeracion;
	}

	@Override
	public List<NumeracionPuntoEmision> selectByPuntoEmision(Long idPuntoEmision) throws Throwable {
		System.out.println("NumeracionPuntoEmisionServiceImpl.selectByPuntoEmision - idPuntoEmision: " + idPuntoEmision);
		return numeracionDaoService.selectByPuntoEmision(idPuntoEmision);
	}

	@Override
	public NumeracionPuntoEmision selectByPuntoEmisionTipo(Long idPuntoEmision, String tipoDoc) throws Throwable {
		System.out.println("NumeracionPuntoEmisionServiceImpl.selectByPuntoEmisionTipo - idPuntoEmision: " + idPuntoEmision + ", tipoDoc: " + tipoDoc);
		return numeracionDaoService.selectByPuntoEmisionTipo(idPuntoEmision, tipoDoc);
	}

	@Override
	public Long obtenerSiguienteNumero(Long idPuntoEmision, String tipoDoc) throws Throwable {
		System.out.println("NumeracionPuntoEmisionServiceImpl.obtenerSiguienteNumero - idPuntoEmision: " + idPuntoEmision + ", tipoDoc: " + tipoDoc);
		
		NumeracionPuntoEmision numeracion = numeracionDaoService.selectByPuntoEmisionTipo(idPuntoEmision, tipoDoc);
		
		if (numeracion == null) {
			// Si no existe, crear nueva numeración empezando en 1
			numeracion = new NumeracionPuntoEmision();
			numeracion.setPtoEmision(null); // Se debe setear desde el servicio que llama
			numeracion.setTipoDoc(tipoDoc);
			numeracion.setNumActual(1L);
			numeracion = numeracionDaoService.save(numeracion, null);
			return 1L;
		}
		
		// Incrementar el número actual
		Long siguienteNumero = numeracion.getNumActual() + 1;
		numeracion.setNumActual(siguienteNumero);
		numeracionDaoService.save(numeracion, numeracion.getId());
		
		return siguienteNumero;
	}
}
