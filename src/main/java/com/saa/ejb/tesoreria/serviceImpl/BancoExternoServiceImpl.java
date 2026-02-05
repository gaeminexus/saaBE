package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.BancoExternoDaoService;
import com.saa.ejb.tesoreria.service.BancoExternoService;
import com.saa.model.tsr.BancoExterno;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz BancoExternoService.
 *  Contiene los servicios relacionados con la entidad BancoExterno</p>
 */
@Stateless
public class BancoExternoServiceImpl implements BancoExternoService {
	
	@EJB
	private BancoExternoDaoService bancoExternoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de banco externo service");
		//INSTANCIA UNA ENTIDAD
		BancoExterno bancoExterno = new BancoExterno();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			bancoExternoDaoService.remove(bancoExterno, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.Object[][])
	 */
	public void save(List<BancoExterno> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de banco externo service");
		for (BancoExterno registro : lista) {			
			bancoExternoDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	public List<BancoExterno> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) BancoExternoService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<BancoExterno> result = bancoExternoDaoService.selectAll(NombreEntidadesTesoreria.BANCO_EXTERNO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total BancoExterno no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<BancoExterno> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) BancoExterno");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<BancoExterno> result = bancoExternoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.BANCO_EXTERNO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total BancoExterno no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectById(java.lang.Long)
	 */
	public BancoExterno selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return bancoExternoDaoService.selectById(id, NombreEntidadesTesoreria.BANCO_EXTERNO);
	}

	@Override
	public BancoExterno saveSingle(BancoExterno bancoExterno) throws Throwable {
		System.out.println("saveSingle - BancoExterno");
		bancoExterno = bancoExternoDaoService.save(bancoExterno, bancoExterno.getCodigo());
		return bancoExterno;
	}
}
