package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.GrupoCajaDaoService;
import com.saa.ejb.tsr.service.GrupoCajaService;
import com.saa.model.tsr.GrupoCaja;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz GrupoCajaService.
 *  Contiene los servicios relacionados con la entidad GrupoCaja</p>
 */
@Stateless
public class GrupoCajaServiceImpl implements GrupoCajaService{

	@EJB
	private GrupoCajaDaoService grupoCajaDaoService;
	
	@EJB
	private DetalleRubroService detalleRubroService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de grupo Caja service");
		GrupoCaja grupoCaja = new GrupoCaja();
		for (Long registro : id) {
			grupoCajaDaoService.remove(grupoCaja, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<GrupoCaja>)
	 */
	public void save(List<GrupoCaja> list) throws Throwable {
		System.out.println("Ingresa al metodo save de grupo Caja service");
		GrupoCaja grupoCaja = new GrupoCaja();
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (GrupoCaja registro : list) {			
			grupoCaja.setFechaIngreso(LocalDateTime.now());
			grupoCajaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<GrupoCaja> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) GrupoCajaService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<GrupoCaja> result = grupoCajaDaoService.selectAll(NombreEntidadesTesoreria.GRUPO_CAJA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total GrupoCaja no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<GrupoCaja> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) GrupoCaja");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<GrupoCaja> result = grupoCajaDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.GRUPO_CAJA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total GrupoCaja no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.GrupoCajaService#selectById(java.lang.Long)
	 */
	public GrupoCaja selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return grupoCajaDaoService.selectById(id, NombreEntidadesTesoreria.GRUPO_CAJA);
	}

	@Override
	public GrupoCaja saveSingle(GrupoCaja grupoCaja) throws Throwable {
		System.out.println("saveSingle - grupoCaja");
		grupoCaja = grupoCajaDaoService.save(grupoCaja, grupoCaja.getCodigo());
		return grupoCaja;
	}
}
