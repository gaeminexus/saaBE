package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.FacturadorDaoService;
import com.saa.ejb.cxc.service.FacturadorService;
import com.saa.model.cxc.Facturador;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio para la entidad Facturador
 */
@Stateless
public class FacturadorServiceImpl implements FacturadorService {
	
	@EJB
	private FacturadorDaoService facturadorDaoService;

	@Override
	public void save(List<Facturador> lista) throws Throwable {
		System.out.println("FacturadorServiceImpl.save - cantidad: " + lista.size());
		for (Facturador facturador : lista) {
			facturadorDaoService.save(facturador, facturador.getId());
		}
	}

	@Override
	public void remove(List<Long> ids) throws Throwable {
		System.out.println("FacturadorServiceImpl.remove - cantidad: " + ids.size());
		Facturador facturador = new Facturador();
		for (Long id : ids) {
			facturadorDaoService.remove(facturador, id);
		}
	}

	@Override
	public List<Facturador> selectAll() throws Throwable {
		System.out.println("FacturadorServiceImpl.selectAll");
		List<Facturador> result = facturadorDaoService.selectAll(NombreEntidadesCobro.FACTURADOR);
		if (result.isEmpty()) {
			throw new IncomeException("Búsqueda completa de Facturador no devolvió ningún registro");
		}
		return result;
	}

	@Override
	public Facturador selectById(Long id) throws Throwable {
		System.out.println("FacturadorServiceImpl.selectById - id: " + id);
		return facturadorDaoService.selectById(id, NombreEntidadesCobro.FACTURADOR);
	}

	@Override
	public List<Facturador> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("FacturadorServiceImpl.selectByCriteria");
		List<Facturador> result = facturadorDaoService.selectByCriteria(datos, NombreEntidadesCobro.FACTURADOR);
		if (result.isEmpty()) {
			throw new IncomeException("Búsqueda por criterio de Facturador no devolvió ningún registro");
		}
		return result;
	}

	@Override
	public Facturador saveSingle(Facturador facturador) throws Throwable {
		System.out.println("FacturadorServiceImpl.saveSingle");
		facturador = facturadorDaoService.save(facturador, facturador.getId());
		return facturador;
	}

	@Override
	public Facturador selectByNumDoc(String numDoc) throws Throwable {
		System.out.println("FacturadorServiceImpl.selectByNumDoc - numDoc: " + numDoc);
		return facturadorDaoService.selectByNumDoc(numDoc);
	}
}
