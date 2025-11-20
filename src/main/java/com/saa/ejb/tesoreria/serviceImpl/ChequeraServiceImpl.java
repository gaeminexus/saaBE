package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.ChequeraDaoService;
import com.saa.ejb.tesoreria.service.ChequeraService;
import com.saa.model.tesoreria.Chequera;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ChequeraService.
 *  Contiene los servicios relacionados con la entidad Chequera.</p>
 */
@Stateless
public class ChequeraServiceImpl implements ChequeraService {
	
	@EJB
	private ChequeraDaoService chequeraDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de chequera service");
		Chequera chequera = new Chequera();
		for (Long registro : id) {
				chequeraDaoService.remove(chequera, registro);	
		}		
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.List<Chequera>)
	 */
	public void save(List<Chequera> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de chequera service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Chequera registro : lista) {			
			chequeraDaoService.save(registro, registro.getCodigo()); 
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	public List<Chequera> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) chequera Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Chequera> result = chequeraDaoService.selectAll(NombreEntidadesTesoreria.CHEQUERA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CajaLogicaPorCajaFisica no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Chequera> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Chequera");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Chequera> result = chequeraDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.CHEQUERA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CajaLogicaPorCajaFisica no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeraService#selectById(java.lang.Long)
	 */
	public Chequera selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return chequeraDaoService.selectById(id, NombreEntidadesTesoreria.CHEQUERA);
	}

	@Override
	public Chequera saveSingle(Chequera  chequera) throws Throwable {
		System.out.println("saveSingle - Chequera");
		chequera = chequeraDaoService.save(chequera, chequera.getCodigo());
		return chequera;
	}
	
}
