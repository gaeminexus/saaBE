package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.CajaLogicaPorCajaFisicaDaoService;
import com.saa.ejb.tesoreria.service.CajaLogicaPorCajaFisicaService;
import com.saa.model.tesoreria.CajaLogicaPorCajaFisica;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CajaLogicaPorCajaFisicaService.
 *  Contiene los servicios relacionados con la entidad CajaLogicaPorCajaFisica.</p>
 */
@Stateless
public class CajaLogicaPorCajaFisicaServiceImpl implements CajaLogicaPorCajaFisicaService {
	
	@EJB
	private CajaLogicaPorCajaFisicaDaoService cajaLogicaPorCajaFisicaDaoService;
	
	@EJB
	private DetalleRubroService detalleRubroService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaPorCajaFisicaService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cajaLogicaPorCajaFisica service");
		CajaLogicaPorCajaFisica cajaLogicaPorCajaFisica = new CajaLogicaPorCajaFisica();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			cajaLogicaPorCajaFisicaDaoService.remove(cajaLogicaPorCajaFisica, registro);	
		}		
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaPorCajaFisicaService#save(java.lang.List<CajaLogicaPorCajaFisica>)
	 */
	public void save(List<CajaLogicaPorCajaFisica> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cajaLogicaPorCajaFisica service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (CajaLogicaPorCajaFisica registro : lista) {			
			registro.setFechaIngreso(LocalDateTime.now());
			cajaLogicaPorCajaFisicaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaPorCajaFisicaService#selectAll()
	 */
	public List<CajaLogicaPorCajaFisica> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cajaLogicaPorCajaFisica Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CajaLogicaPorCajaFisica> result = cajaLogicaPorCajaFisicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_LOGICA_POR_CAJA_FISICA); 
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
	public List<CajaLogicaPorCajaFisica> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CajaLogicaPorCajaFisica");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CajaLogicaPorCajaFisica> result = cajaLogicaPorCajaFisicaDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.CAJA_LOGICA_POR_CAJA_FISICA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CajaLogicaPorCajaFisica no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaPorCajaFisicaService#selectById(java.lang.Long)
	 */
	public CajaLogicaPorCajaFisica selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cajaLogicaPorCajaFisicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_LOGICA_POR_CAJA_FISICA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaPorCajaFisicaService#recuperaNumeroCajasLogicas(java.lang.Long)
	 */
	public Long[] recuperaNumeroCajasLogicas(Long idCajaFisica) throws Throwable {
		System.out.println("Ingresa al metodo recuperaNumeroCajasLogicas de cajaLogica: " + idCajaFisica);
		CajaLogicaPorCajaFisica cajaLogicaPorCajaFisica = null;
		Long[] cajasLogicas = new Long[2];
		cajasLogicas[0] = cajaLogicaPorCajaFisicaDaoService.recuperaNumeroCajas(idCajaFisica);
		cajasLogicas[1] = 0L;
		if(cajasLogicas[0].equals(0L)){
			throw new IncomeException("EL USUARIO NO TIENE CAJAS ASIGNADAS");
		}
		if(cajasLogicas[0].equals(1L)){
			cajaLogicaPorCajaFisica = cajaLogicaPorCajaFisicaDaoService.selectByCajaFisica(idCajaFisica);
			cajasLogicas[1] = cajaLogicaPorCajaFisica.getCajaLogica().getCodigo();
		}
		return cajasLogicas;
	}

	@Override
	public CajaLogicaPorCajaFisica saveSingle(CajaLogicaPorCajaFisica cajaLogicaPorCajaFisica) throws Throwable {
		System.out.println("saveSingle - cajaLogicaPorCajaFisica");
		cajaLogicaPorCajaFisica = cajaLogicaPorCajaFisicaDaoService.save(cajaLogicaPorCajaFisica, cajaLogicaPorCajaFisica.getCodigo());
		return cajaLogicaPorCajaFisica;
	}
}