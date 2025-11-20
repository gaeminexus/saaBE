package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.CobroChequeDaoService;
import com.saa.ejb.tesoreria.dao.DetalleDepositoDaoService;
import com.saa.ejb.tesoreria.service.CobroChequeService;
import com.saa.model.tesoreria.Cobro;
import com.saa.model.tesoreria.CobroCheque;
import com.saa.model.tesoreria.DetalleDeposito;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroCheque;
import com.saa.rubros.EstadoCobroCheque;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CobroChequeService.
 *  Contiene los servicios relacionados con la entidad CobroCheque.</p>
 */
@Stateless
public class CobroChequeServiceImpl implements CobroChequeService {
	
	@EJB
	private CobroChequeDaoService cobroChequeDaoService;
	
	@EJB
	private DetalleDepositoDaoService detalleDepositoDaoService;
	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroChequeService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cobroCheque service");
		CobroCheque cobroCheque = new CobroCheque();
		for (Long registro : id) {
			cobroChequeDaoService.remove(cobroCheque, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroChequeService#save(java.lang.List<CobroCheque>)
	 */
	public void save(List<CobroCheque> list) throws Throwable {
		System.out.println("Ingresa al metodo save de cobroCheque service");
		for (CobroCheque cobroCheque : list) {			
			cobroChequeDaoService.save(cobroCheque, cobroCheque.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroChequeService#selectAll()
	 */
	public List<CobroCheque> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cobroCheque Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroCheque> result = cobroChequeDaoService.selectAll(NombreEntidadesTesoreria.COBRO_CHEQUE); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CobroCheque no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CobroCheque> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CobroCheque");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroCheque> result = cobroChequeDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.COBRO_CHEQUE); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CobroCheque no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroChequeService#selectById(java.lang.Long)
	 */
	public CobroCheque selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cobroChequeDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_CHEQUE);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroChequeService#saveCobroChequeReal(com.compuseg.income.tesoreria.ejb.model.TempCobroCheque, com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public void saveCobroChequeReal(TempCobroCheque tempCobroCheque, Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo saveCobroChequeReal con idCobro: " + cobro.getCodigo());
		CobroCheque cobroCheque = new CobroCheque();
		cobroCheque.setCodigo(0L);
		cobroCheque.setCobro(cobro);
		cobroCheque.setBancoExterno(tempCobroCheque.getBancoExterno());
		cobroCheque.setNumero(tempCobroCheque.getNumero());
		cobroCheque.setValor(tempCobroCheque.getValor());
		cobroCheque.setEstado(1L);
		try {
			cobroChequeDaoService.save(cobroCheque, cobroCheque.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en saveCobroEfectivoReal: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroChequeService#actualizaCobroCheque(java.lang.Long, com.compuseg.income.tesoreria.ejb.model.DetalleDeposito, int)
	 */
	public void actualizaCobroCheque(Long idDetalleDeposito, int estado) throws Throwable {
		System.out.println("Ingresa al metodo actualizaCobroCheque con id detalle deposito: " + idDetalleDeposito +", estado: "+estado);
		List<CobroCheque> lista = cobroChequeDaoService.selectByDetalleDeposito(idDetalleDeposito);
		if(!lista.isEmpty()){
			for(CobroCheque cobroCheque : lista){
				cobroCheque.setEstado(Long.valueOf(estado));
				if(estado == EstadoCobroCheque.ACTIVO){
					cobroCheque.setDetalleDeposito(null);					
				}
				else{
					DetalleDeposito detalleDeposito = detalleDepositoDaoService.selectById(idDetalleDeposito, NombreEntidadesTesoreria.DETALLE_DEPOSITO);
					cobroCheque.setDetalleDeposito(detalleDeposito);					
				}
					
				try {					
					cobroChequeDaoService.save(cobroCheque, cobroCheque.getCodigo());					
				} catch (PersistenceException e) {
					throw new IncomeException("Error en actualizaCobroCheque: " + e.getCause());
				}	
			}
		}			
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroChequeService#sumaValorCheque(java.lang.Long)
	 */
	public Double sumaValorCheque(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo sumaValorCheque con id cobro: " + idCobro);
		return cobroChequeDaoService.selectSumaByCobro(idCobro);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroChequeService#actualizaCobroCheque(com.compuseg.income.tesoreria.ejb.model.CobroCheque, com.compuseg.income.tesoreria.ejb.model.DetalleDeposito, int)
	 */
	public void actualizaCobroCheque(CobroCheque cobroCheque, DetalleDeposito detalleDeposito, int estado) throws Throwable {
		System.out.println("Ingresa al metodo actualizaCobroCheque con id : " + cobroCheque.getCodigo() +", estado: "+estado);		
		cobroCheque.setEstado(Long.valueOf(estado));
		cobroCheque.setDetalleDeposito(detalleDeposito);
		
		try {
			cobroChequeDaoService.save(cobroCheque, cobroCheque.getCodigo());			
		} catch (PersistenceException e) {
			throw new IncomeException("Error en actualizaCobroCheque: " + e.getCause());
		}			
	}

	@Override
	public CobroCheque saveSingle(CobroCheque cobroCheque) throws Throwable {
		System.out.println("saveSingle - CobroCheque");
		cobroCheque = cobroChequeDaoService.save(cobroCheque, cobroCheque.getCodigo());
		return cobroCheque;
	}
}
