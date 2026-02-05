package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.CobroEfectivoDaoService;
import com.saa.ejb.tsr.service.CobroEfectivoService;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.CobroEfectivo;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempCobroEfectivo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CobroEfectivoService.
 *  Contiene los servicios relacionados con la entidad CobroEfectivo.</p>
 */
@Stateless
public class CobroEfectivoServiceImpl implements CobroEfectivoService {
	
	@EJB
	private CobroEfectivoDaoService cobroEfectivoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroEfectivoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cobroEfectivo service");
		CobroEfectivo cobroEfectivo = new CobroEfectivo();
		for (Long registro : id) {
			cobroEfectivoDaoService.remove(cobroEfectivo, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroEfectivoService#save(java.lang.List<CobroEfectivo>)
	 */
	public void save(List<CobroEfectivo> object) throws Throwable {
		System.out.println("Ingresa al metodo save de cobroEfectivo service");
		for (CobroEfectivo registro : object) {			
			cobroEfectivoDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroEfectivoService#selectAll()
	 */
	public List<CobroEfectivo> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cobroEfectivo Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroEfectivo> result = cobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.COBRO_EFECTIVO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CobroEfectivo no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CobroEfectivo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CobroEfectivo");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroEfectivo> result = cobroEfectivoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.COBRO_EFECTIVO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CobroEfectivo no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroEfectivoService#selectById(java.lang.Long)
	 */
	public CobroEfectivo selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cobroEfectivoDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_EFECTIVO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroEfectivoService#saveCobroEfectivoReal(com.compuseg.income.tesoreria.ejb.model.TempCobroEfectivo, com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public void saveCobroEfectivoReal(TempCobroEfectivo tempCobroEfectivo, Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo saveCobroEfectivoReal con idCobro: " + cobro.getCodigo()+", temporal de cobro efectivo: "+tempCobroEfectivo.getCodigo());
		CobroEfectivo cobroEfectivo = new CobroEfectivo();
		cobroEfectivo.setCodigo(0L);
		cobroEfectivo.setCobro(cobro);
		cobroEfectivo.setValor(tempCobroEfectivo.getValor());
		try {
			cobroEfectivoDaoService.save(cobroEfectivo, cobroEfectivo.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en saveCobroEfectivoReal: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroEfectivoService#sumaValorEfectivo(java.lang.Long)
	 */
	public Double sumaValorEfectivo(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo sumaValorCheque con id cobro: " + idCobro);
		return cobroEfectivoDaoService.selectSumaByCobro(idCobro);
	}

	@Override
	public CobroEfectivo saveSingle(CobroEfectivo cobroEfectivo) throws Throwable {
		System.out.println("saveSingle - CobroCheque");
		cobroEfectivo = cobroEfectivoDaoService.save(cobroEfectivo, cobroEfectivo.getCodigo());
		return cobroEfectivo;
	}

}
