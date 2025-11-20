package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.CajaLogicaDaoService;
import com.saa.ejb.tesoreria.service.CajaLogicaService;
import com.saa.model.contabilidad.PlanCuenta;
import com.saa.model.tesoreria.CajaLogica;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CajaLogicaService.
 *  Contiene los servicios relacionados con la entidad CajaLogica.</p>
 */
@Stateless
public class CajaLogicaServiceImpl implements CajaLogicaService {
	
	@EJB
	private CajaLogicaDaoService cajaLogicaDaoService;
	
	@EJB
	private DetalleRubroService detalleRubroService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cajaLogica service");
		//INSTANCIA UNA ENTIDAD
		CajaLogica cajaLogica = new CajaLogica();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			cajaLogicaDaoService.remove(cajaLogica, registro);
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaService#save(java.lang.List<CajaLogica>)
	 */
	public void save(List<CajaLogica> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cajaLogica service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (CajaLogica registro : lista) {			
			registro.setFechaIngreso(LocalDateTime.now());
			cajaLogicaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaService#selectAll()
	 */
	public List<CajaLogica> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cajaLogica Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CajaLogica> result = cajaLogicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_LOGICA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total cajaLogica no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CajaLogica> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CajaLogica");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CajaLogica> result = cajaLogicaDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.CAJA_LOGICA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CajaLogicaPorCajaFisica no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaService#selectById(java.lang.Long)
	 */
	public CajaLogica selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cajaLogicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_LOGICA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaLogicaService#recuperaCuentaContable(java.lang.Long)
	 */
	public PlanCuenta recuperaCuentaContable(Long idCaja) throws Throwable {
		System.out.println("Ingresa al recuperaCuentaContable con id: " + idCaja);
		return cajaLogicaDaoService.recuperaCuentaContable(idCaja);
	}

	@Override
	public CajaLogica saveSingle(CajaLogica cajaLogica) throws Throwable {
		System.out.println("saveSingle - CajaLogica");
		cajaLogica = cajaLogicaDaoService.save(cajaLogica, cajaLogica.getCodigo());
		return cajaLogica;
	}
}
