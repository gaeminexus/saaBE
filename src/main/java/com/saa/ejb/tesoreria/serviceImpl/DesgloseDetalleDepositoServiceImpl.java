package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.DesgloseDetalleDepositoDaoService;
import com.saa.ejb.tesoreria.service.DesgloseDetalleDepositoService;
import com.saa.model.tesoreria.AuxDepositoDesglose;
import com.saa.model.tesoreria.DesgloseDetalleDeposito;
import com.saa.model.tesoreria.DetalleDeposito;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.PersistenceException;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DesgloseDetalleDepositoService.
 *  Contiene los servicios relacionados con la entidad DesgloseDetalleDeposito.</p>
 */
@Stateless
public class DesgloseDetalleDepositoServiceImpl implements DesgloseDetalleDepositoService {
	
	@EJB
	private DesgloseDetalleDepositoDaoService desgloseDetalleDepositoDaoService;
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DesgloseDetalleDepositoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de desgloseDetalleDeposito service");
		DesgloseDetalleDeposito desgloseDetalleDeposito = new DesgloseDetalleDeposito();
		for (Long registro : id) {
			desgloseDetalleDepositoDaoService.remove(desgloseDetalleDeposito, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DesgloseDetalleDepositoService#save(java.lang.List<DesgloseDetalleDeposito>)
	 */
	public void save(List<DesgloseDetalleDeposito> list) throws Throwable {
		System.out.println("Ingresa al metodo save de desgloseDetalleDeposito service");
		for (DesgloseDetalleDeposito registro : list) {			
			desgloseDetalleDepositoDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DesgloseDetalleDepositoService#selectAll()
	 */
	public List<DesgloseDetalleDeposito> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) desgloseDetalleDeposito Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DesgloseDetalleDeposito> result = desgloseDetalleDepositoDaoService.selectAll(NombreEntidadesTesoreria.DESGLOSE_DETALLE_DEPOSITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DesgloseDetalleDeposito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DesgloseDetalleDeposito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DesgloseDetalleDeposito");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DesgloseDetalleDeposito> result = desgloseDetalleDepositoDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.DESGLOSE_DETALLE_DEPOSITO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total DesgloseDetalleDeposito no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	public DesgloseDetalleDeposito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return desgloseDetalleDepositoDaoService.selectById(id, NombreEntidadesTesoreria.DESGLOSE_DETALLE_DEPOSITO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DesgloseDetalleDepositoService#saveDesglose(com.compuseg.income.tesoreria.ejb.model.AuxDepositoDesglose, com.compuseg.income.tesoreria.ejb.model.DetalleDeposito)
	 */
	public void saveDesglose(AuxDepositoDesglose auxDepositoDesglose, DetalleDeposito detalleDeposito) throws Throwable {
		System.out.println("Ingresa al saveDesglose con id AuxDepositoDesglose: " + auxDepositoDesglose.getCodigo() + ", id DetalleDeposito: " + detalleDeposito.getCodigo());
		DesgloseDetalleDeposito desgloseDetalleDeposito = new DesgloseDetalleDeposito();
		desgloseDetalleDeposito.setCodigo(0L);
		desgloseDetalleDeposito.setDetalleDeposito(detalleDeposito);
		desgloseDetalleDeposito.setTipo(auxDepositoDesglose.getTipo());
		desgloseDetalleDeposito.setValor(auxDepositoDesglose.getValor());
		if(auxDepositoDesglose.getCobro() != null)
			desgloseDetalleDeposito.setCobro(auxDepositoDesglose.getCobro());
		desgloseDetalleDeposito.setBancoExterno(auxDepositoDesglose.getBancoExterno());
		desgloseDetalleDeposito.setNumeroCheque(auxDepositoDesglose.getNumeroCheque());
		if(auxDepositoDesglose.getCobroCheque() != null)
			desgloseDetalleDeposito.setCobroCheque(auxDepositoDesglose.getCobroCheque());
		try {
			desgloseDetalleDepositoDaoService.save(desgloseDetalleDeposito, desgloseDetalleDeposito.getCodigo());
		} catch (PersistenceException e) {
			throw new IncomeException("Error en el metodo saveDesglose: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.DesgloseDetalleDepositoService#selectDetallesRatificadosByDeposito(java.lang.Long)
	 */
	public List<DesgloseDetalleDeposito> selectDetallesRatificadosByDeposito( Long idDeposito) throws Throwable {
		System.out.println("Ingresa al metodo selectDetallesRatificadosByDeposito con id deposito: " + idDeposito);
		return desgloseDetalleDepositoDaoService.selectDetallesRatificadosByDeposito(idDeposito);		
	}

	@Override
	public DesgloseDetalleDeposito saveSingle(DesgloseDetalleDeposito desgloseDetalleDeposito) throws Throwable {
		System.out.println("saveSingle - Deposito");
		desgloseDetalleDeposito = desgloseDetalleDepositoDaoService.save(desgloseDetalleDeposito, desgloseDetalleDeposito.getCodigo());
		return desgloseDetalleDeposito;
	}
}
