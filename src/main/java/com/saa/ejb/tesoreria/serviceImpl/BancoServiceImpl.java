package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.BancoDaoService;
import com.saa.ejb.tesoreria.service.BancoService;
import com.saa.model.tesoreria.Banco;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz BancoService.
 *  Contiene los servicios relacionados con la entidad Banco</p>
 */
@Stateless
public class BancoServiceImpl implements BancoService {
	
	@EJB
	private BancoDaoService bancoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.Object[][])
	 */
	public void save(List<Banco> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de banco externo service");
		for (Banco registro : lista) {			
			bancoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de banco externo service");
		//INSTANCIA UNA ENTIDAD
		Banco banco = new Banco();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			bancoDaoService.remove(banco, registro);	
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<Banco> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Banco");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Banco> result = bancoDaoService.selectAll(NombreEntidadesTesoreria.BANCO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Banco no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Banco> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Banco");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Banco> result = bancoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.BANCO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total BancoExterno no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoService#selectById(java.lang.Long)
	 */
	public Banco selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return bancoDaoService.selectById(id, NombreEntidadesTesoreria.BANCO);
	}

	@Override
	public Banco saveSingle(Banco banco) throws Throwable {
		System.out.println("saveSingle - Banco");
		banco = bancoDaoService.save(banco, banco.getCodigo());
		return banco;
	}
}