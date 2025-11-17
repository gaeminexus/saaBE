package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.CobroTransferenciaDaoService;
import com.saa.ejb.tesoreria.service.CobroTransferenciaService;
import com.saa.model.tesoreria.Cobro;
import com.saa.model.tesoreria.CobroTransferencia;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroTransferencia;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CobroTransferenciaService.
 *  Contiene los servicios relacionados con la entidad CobroTransferencia.</p>
 */
@Stateless
public class CobroTransferenciaServiceImpl implements CobroTransferenciaService {
	
	@EJB
	private CobroTransferenciaDaoService cobroTransferenciaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTransferenciaService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cobroTransferencia service");
		CobroTransferencia cobroTransferencia = new CobroTransferencia();
		for (Long registro : id) {
			cobroTransferenciaDaoService.remove(cobroTransferencia, registro);;	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTransferenciaService#save(java.lang.List<CobroTransferencia>)
	 */
	public void save(List<CobroTransferencia> list) throws Throwable {
		System.out.println("Ingresa al metodo save de cobroTransferencia service");
		for (CobroTransferencia registro : list) {			
			cobroTransferenciaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTransferenciaService#selectAll()
	 */
	public List<CobroTransferencia> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cobroTransferencia Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroTransferencia> result = cobroTransferenciaDaoService.selectAll(NombreEntidadesTesoreria.COBRO_TRANSFERENCIA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CobroTarjeta no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CobroTransferencia> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CobroTransferencia");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroTransferencia> result = cobroTransferenciaDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.COBRO_TRANSFERENCIA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CobroTarjeta no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTransferenciaService#selectById(java.lang.Long)
	 */
	public CobroTransferencia selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cobroTransferenciaDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_TRANSFERENCIA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTransferenciaService#saveCobroTransferenciaReal(com.compuseg.income.tesoreria.ejb.model.TempCobroTransferencia, com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public void saveCobroTransferenciaReal( TempCobroTransferencia tempCobroTransferencia, Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo saveCobroTransferenciaReal con idCobro: " + cobro.getCodigo());
		CobroTransferencia cobroTransferencia = new CobroTransferencia();
		cobroTransferencia.setCodigo(0L);
		cobroTransferencia.setCobro(cobro);
		cobroTransferencia.setBancoExterno(tempCobroTransferencia.getBancoExterno());
		cobroTransferencia.setCuentaOrigen(tempCobroTransferencia.getCuentaOrigen());
		cobroTransferencia.setNumeroTransferencia(tempCobroTransferencia.getNumeroTransferencia());
		cobroTransferencia.setBanco(tempCobroTransferencia.getBanco());
		cobroTransferencia.setCuentaBancaria(tempCobroTransferencia.getCuentaBancaria());
		cobroTransferencia.setCuentaDestino(tempCobroTransferencia.getCuentaDestino());
		cobroTransferencia.setValor(tempCobroTransferencia.getValor());
		try {
			cobroTransferenciaDaoService.save(cobroTransferencia, cobroTransferencia.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en saveCobroEfectivoReal: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTransferenciaService#sumaValorTransferencia(java.lang.Long)
	 */
	public Double sumaValorTransferencia(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo sumaValorTransferencia con id cobro: " + idCobro);
		return cobroTransferenciaDaoService.selectSumaByCobro(idCobro);
	}

	@Override
	public CobroTransferencia saveSingle(CobroTransferencia cobroTransferencia) throws Throwable {
		System.out.println("saveSingle - CobroTransferencia");
		cobroTransferencia = cobroTransferenciaDaoService.save(cobroTransferencia, cobroTransferencia.getCodigo());
		return cobroTransferencia;
	}
}