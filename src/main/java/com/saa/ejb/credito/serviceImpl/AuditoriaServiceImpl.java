package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.AuditoriaDaoService;
import com.saa.ejb.credito.service.AuditoriaService;
import com.saa.model.credito.Auditoria;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class AuditoriaServiceImpl implements AuditoriaService {
	
	@EJB
	private AuditoriaDaoService auditoriaDaoService;

	@Override
	public void remove(List<Long> ids) throws Throwable {
		System.out.println("remove[] - Auditoria");
		Auditoria entidad = new Auditoria();
		for (Long id : ids) {
			auditoriaDaoService.remove(entidad, id);
		}

	}

	@Override
	public void save(List<Auditoria> object) throws Throwable {
		System.out.println("save[] - Auditoria");
		for (Auditoria registro : object) {
			auditoriaDaoService.save(registro, registro.getCodigo());
		}
		
	}

	@Override
	public Auditoria saveSingle(Auditoria object) throws Throwable {
		System.out.println("saveSingle - Auditoria");
		if (object.getCodigo() == null) {
			return auditoriaDaoService.save(object, null);
		} else {
			return auditoriaDaoService.save(object, object.getCodigo());
		}		
	}

	@Override
	public List<Auditoria> selectAll() throws Throwable {
		System.out.println("selectAll - Auditoria");
		List<Auditoria> result = 
				auditoriaDaoService.selectAll(NombreEntidadesCredito.AUDITORIA);
		if (result.isEmpty())
			throw new Throwable("No existen registros en Auditoria");
		return result;
	}

	@Override
	public Auditoria selectById(Long id) throws Throwable {
		System.out.println("selectById - Auditoria id: " + id);
		return auditoriaDaoService.selectById(id, NombreEntidadesCredito.AUDITORIA);
	}

	@Override
	public List<Auditoria> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("selectByCriteria - Auditoria");
		List<Auditoria> result = 
				auditoriaDaoService.selectByCriteria(datos, NombreEntidadesCredito.AUDITORIA);
		if (result.isEmpty())
			throw new Throwable("No existen registros en Auditoria con los criterios indicados");
		return result;
	}
	

}
