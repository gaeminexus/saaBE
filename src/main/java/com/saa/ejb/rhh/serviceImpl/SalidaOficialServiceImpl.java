package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.SalidaOficialDaoService;
import com.saa.ejb.rhh.service.SalidaOficialService;
import com.saa.model.rhh.SalidaOficial;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz SalidaOficialService.
 *  Contiene los servicios relacionados con la entidad SalidaOficial.</p>
 */
@Stateless
public class SalidaOficialServiceImpl implements SalidaOficialService {

	@EJB
	private SalidaOficialDaoService salidaOficialDaoService;

	@Override
	public void save(List<SalidaOficial> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de salidaOficial service");
		for (SalidaOficial registro : lista) {
			salidaOficialDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de salidaOficial service");
		//INSTANCIA UNA ENTIDAD
		SalidaOficial salidaOficial = new SalidaOficial();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			salidaOficialDaoService.remove(salidaOficial, registro);
		}
	}

	@Override
	public List<SalidaOficial> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) SalidaOficial");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SalidaOficial> result = salidaOficialDaoService.selectAll(NombreEntidadesRhh.SALIDA_OFICIAL);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de salidaOficial no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public SalidaOficial selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de salidaOficial con id: " + id);
		return salidaOficialDaoService.selectById(id, NombreEntidadesRhh.SALIDA_OFICIAL);
	}

	@Override
	public List<SalidaOficial> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) SalidaOficial");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SalidaOficial> result = salidaOficialDaoService.selectByCriteria(datos, NombreEntidadesRhh.SALIDA_OFICIAL);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de salidaOficial no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public SalidaOficial saveSingle(SalidaOficial salidaOficial) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) SalidaOficial");
		salidaOficial = salidaOficialDaoService.save(salidaOficial, salidaOficial.getCodigo());
		return salidaOficial;
	}
}
