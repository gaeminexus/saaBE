package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.CobroRetencionDaoService;
import com.saa.ejb.tsr.service.CobroRetencionService;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.CobroRetencion;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempCobroRetencion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CobroRetencionService.
 *  Contiene los servicios relacionados con la entidad CobroRetencion.</p>
 */
@Stateless
public class CobroRetencionServiceImpl implements CobroRetencionService {
	
	@EJB
	private CobroRetencionDaoService cobroRetencionDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroRetencionService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cobroRetencion service");
		CobroRetencion cobroRetencion = new CobroRetencion();
		for (Long registro : id) {
			cobroRetencionDaoService.remove(cobroRetencion, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroRetencionService#save(java.lang.List<CobroRetencion>)
	 */
	public void save(List<CobroRetencion> object) throws Throwable {
		System.out.println("Ingresa al metodo save de cobroRetencion service");
		for (CobroRetencion registro : object) {			
			cobroRetencionDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroRetencionService#selectAll()
	 */
	public List<CobroRetencion> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cobroRetencion Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroRetencion> result = cobroRetencionDaoService.selectAll(NombreEntidadesTesoreria.COBRO_RETENCION); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CobroRetencion no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CobroRetencion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CobroRetencion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroRetencion> result = cobroRetencionDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.COBRO_RETENCION); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CobroRetencion no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroRetencionService#selectById(java.lang.Long)
	 */
	public CobroRetencion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cobroRetencionDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_RETENCION);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroRetencionService#saveCobroRetencionReal(com.compuseg.income.tesoreria.ejb.model.TempCobroRetencion, com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public void saveCobroRetencionReal(TempCobroRetencion tempCobroRetencion, Cobro cobro) throws Throwable { 
		System.out.println("Ingresa al metodo saveCobroRetencionReal con idCobro: " + cobro.getCodigo());
		CobroRetencion cobroRetencion = new CobroRetencion();
		cobroRetencion.setCodigo(0L);
		cobroRetencion.setCobro(cobro);
		cobroRetencion.setPlantilla(tempCobroRetencion.getPlantilla());
		cobroRetencion.setDetallePlantilla(tempCobroRetencion.getDetallePlantilla());
		cobroRetencion.setNumero(tempCobroRetencion.getNumero());
		cobroRetencion.setValor(tempCobroRetencion.getValor());
		try {
			cobroRetencionDaoService.save(cobroRetencion, cobroRetencion.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en saveCobroEfectivoReal: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroRetencionService#sumaValorRetencion(java.lang.Long)
	 */
	public Double sumaValorRetencion(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo sumaValorRetencion con id cobro: " + idCobro);
		return cobroRetencionDaoService.selectSumaByCobro(idCobro);
	}

	@Override
	public CobroRetencion saveSingle(CobroRetencion cobroRetencion) throws Throwable {
		System.out.println("saveSingle - CobroRetencion");
		cobroRetencion = cobroRetencionDaoService.save(cobroRetencion, cobroRetencion.getCodigo());
		return cobroRetencion;
	}
}
