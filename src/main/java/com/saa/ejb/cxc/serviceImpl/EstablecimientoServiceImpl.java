package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.EstablecimientoDaoService;
import com.saa.ejb.cxc.service.EstablecimientoService;
import com.saa.model.cxc.Establecimiento;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio para la entidad Establecimiento
 */
@Stateless
public class EstablecimientoServiceImpl implements EstablecimientoService {
	
	@EJB
	private EstablecimientoDaoService establecimientoDaoService;

	@Override
	public void save(List<Establecimiento> lista) throws Throwable {
		System.out.println("EstablecimientoServiceImpl.save - cantidad: " + lista.size());
		for (Establecimiento establecimiento : lista) {
			establecimientoDaoService.save(establecimiento, establecimiento.getId());
		}
	}

	@Override
	public void remove(List<Long> ids) throws Throwable {
		System.out.println("EstablecimientoServiceImpl.remove - cantidad: " + ids.size());
		Establecimiento establecimiento = new Establecimiento();
		for (Long id : ids) {
			establecimientoDaoService.remove(establecimiento, id);
		}
	}

	@Override
	public List<Establecimiento> selectAll() throws Throwable {
		System.out.println("EstablecimientoServiceImpl.selectAll");
		List<Establecimiento> result = establecimientoDaoService.selectAll(NombreEntidadesCobro.ESTABLECIMIENTO);
		if (result.isEmpty()) {
			throw new IncomeException("Búsqueda completa de Establecimiento no devolvió ningún registro");
		}
		return result;
	}

	@Override
	public Establecimiento selectById(Long id) throws Throwable {
		System.out.println("EstablecimientoServiceImpl.selectById - id: " + id);
		return establecimientoDaoService.selectById(id, NombreEntidadesCobro.ESTABLECIMIENTO);
	}

	@Override
	public List<Establecimiento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("EstablecimientoServiceImpl.selectByCriteria");
		List<Establecimiento> result = establecimientoDaoService.selectByCriteria(datos, NombreEntidadesCobro.ESTABLECIMIENTO);
		if (result.isEmpty()) {
			throw new IncomeException("Búsqueda por criterio de Establecimiento no devolvió ningún registro");
		}
		return result;
	}

	@Override
	public Establecimiento saveSingle(Establecimiento establecimiento) throws Throwable {
		System.out.println("EstablecimientoServiceImpl.saveSingle");
		establecimiento = establecimientoDaoService.save(establecimiento, establecimiento.getId());
		return establecimiento;
	}

	@Override
	public List<Establecimiento> selectByFacturador(Long idFacturador) throws Throwable {
		System.out.println("EstablecimientoServiceImpl.selectByFacturador - idFacturador: " + idFacturador);
		return establecimientoDaoService.selectByFacturador(idFacturador);
	}

	@Override
	public Establecimiento selectMatriz(Long idFacturador) throws Throwable {
		System.out.println("EstablecimientoServiceImpl.selectMatriz - idFacturador: " + idFacturador);
		return establecimientoDaoService.selectMatriz(idFacturador);
	}
}
