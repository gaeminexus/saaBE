package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.CajaFisicaDaoService;
import com.saa.ejb.tesoreria.service.CajaFisicaService;
import com.saa.model.tsr.CajaFisica;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CajaFisicaService.
 *  Contiene los servicios relacionados con la entidad CajaFisica.</p>
 */
@Stateless
public class CajaFisicaServiceImpl implements CajaFisicaService {
	
	@EJB
	private CajaFisicaDaoService cajaFisicaDaoService;
	
	@EJB
	private DetalleRubroService detalleRubroService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaFisicaService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cajaFisica service");
		//INSTANCIA UNA ENTIDAD
		CajaFisica  cajaFisica = new CajaFisica();
		for (Long registro : id) {
			cajaFisicaDaoService.remove(cajaFisica, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaFisicaService#save(java.lang.List<CajaFisica>)
	 */
	public void save(List<CajaFisica> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cajaFisica service");
		for (CajaFisica registro : lista) {		
			registro.setFechaIngreso(LocalDateTime.now());
			cajaFisicaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaFisicaService#selectAll()
	 */
	public List<CajaFisica> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cajaFisica Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CajaFisica> result = cajaFisicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_FISICA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CajaFisica no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CajaFisica> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CajaFisica");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CajaFisica> result = cajaFisicaDaoService.selectByCriteria(datos, NombreEntidadesTesoreria.CAJA_FISICA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CajaFisica no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CajaFisicaService#selectById(java.lang.Long)
	 */
	public CajaFisica selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cajaFisicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_FISICA);
	}

	@Override
	public CajaFisica saveSingle(CajaFisica cajaFisica) throws Throwable {
		System.out.println("saveSingle - cajaFisica");
		cajaFisica = cajaFisicaDaoService.save(cajaFisica, cajaFisica.getCodigo());
		return cajaFisica;
	}
}
