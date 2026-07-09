package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.PuntoEmisionDaoService;
import com.saa.ejb.cxc.service.PuntoEmisionService;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PuntoEmision;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio para la entidad PuntoEmision
 */
@Stateless
public class PuntoEmisionServiceImpl implements PuntoEmisionService {
	
	@EJB
	private PuntoEmisionDaoService puntoEmisionDaoService;

	@Override
	public void save(List<PuntoEmision> lista) throws Throwable {
		System.out.println("PuntoEmisionServiceImpl.save - cantidad: " + lista.size());
		for (PuntoEmision puntoEmision : lista) {
			puntoEmisionDaoService.save(puntoEmision, puntoEmision.getId());
		}
	}

	@Override
	public void remove(List<Long> ids) throws Throwable {
		System.out.println("PuntoEmisionServiceImpl.remove - cantidad: " + ids.size());
		PuntoEmision puntoEmision = new PuntoEmision();
		for (Long id : ids) {
			puntoEmisionDaoService.remove(puntoEmision, id);
		}
	}

	@Override
	public List<PuntoEmision> selectAll() throws Throwable {
		System.out.println("PuntoEmisionServiceImpl.selectAll");
		List<PuntoEmision> result = puntoEmisionDaoService.selectAll(NombreEntidadesCobro.PUNTO_EMISION);
		if (result.isEmpty()) {
			throw new IncomeException("Búsqueda completa de PuntoEmision no devolvió ningún registro");
		}
		return result;
	}

	@Override
	public PuntoEmision selectById(Long id) throws Throwable {
		System.out.println("PuntoEmisionServiceImpl.selectById - id: " + id);
		return puntoEmisionDaoService.selectById(id, NombreEntidadesCobro.PUNTO_EMISION);
	}

	@Override
	public List<PuntoEmision> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("PuntoEmisionServiceImpl.selectByCriteria");
		List<PuntoEmision> result = puntoEmisionDaoService.selectByCriteria(datos, NombreEntidadesCobro.PUNTO_EMISION);
		if (result.isEmpty()) {
			throw new IncomeException("Búsqueda por criterio de PuntoEmision no devolvió ningún registro");
		}
		return result;
	}

	@Override
	public PuntoEmision saveSingle(PuntoEmision puntoEmision) throws Throwable {
		System.out.println("PuntoEmisionServiceImpl.saveSingle");
		puntoEmision = puntoEmisionDaoService.save(puntoEmision, puntoEmision.getId());
		return puntoEmision;
	}

	@Override
	public List<PuntoEmision> selectByEstablecimiento(Long idEstablecimiento) throws Throwable {
		System.out.println("PuntoEmisionServiceImpl.selectByEstablecimiento - idEstablecimiento: " + idEstablecimiento);
		return puntoEmisionDaoService.selectByEstablecimiento(idEstablecimiento);
	}
}
