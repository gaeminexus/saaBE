package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.CobroTarjetaDaoService;
import com.saa.ejb.tesoreria.service.CobroTarjetaService;
import com.saa.model.tesoreria.Cobro;
import com.saa.model.tesoreria.CobroTarjeta;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroTarjeta;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CobroTarjetaService.
 *  Contiene los servicios relacionados con la entidad CobroTarjeta.</p>
 */
@Stateless
public class CobroTarjetaServiceImpl implements CobroTarjetaService {
	
	@EJB
	private CobroTarjetaDaoService cobroTarjetaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTarjetaService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de cobroTarjeta service");
		CobroTarjeta cobroTarjeta = new CobroTarjeta();
		for (Long registro : id) {
			cobroTarjetaDaoService.remove(cobroTarjeta, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTarjetaService#save(java.lang.List<CobroTarjeta>)
	 */
	public void save(List<CobroTarjeta> object) throws Throwable {
		System.out.println("Ingresa al metodo save de cobroTarjeta service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (CobroTarjeta registro : object) {			
			cobroTarjetaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTarjetaService#selectAll()
	 */
	public List<CobroTarjeta> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) cobroTarjeta Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroTarjeta> result = cobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.COBRO_TARJETA); 
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
	public List<CobroTarjeta> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CobroTarjeta");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CobroTarjeta> result = cobroTarjetaDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.COBRO_TARJETA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CobroTarjeta no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTarjetaService#selectById(java.lang.Long)
	 */
	public CobroTarjeta selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return cobroTarjetaDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_TARJETA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTarjetaService#saveCobroTarjetaReal(com.compuseg.income.tesoreria.ejb.model.TempCobroTarjeta, com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public void saveCobroTarjetaReal(TempCobroTarjeta tempCobroTarjeta, Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo saveCobroTarjetaReal con idCobro: " + cobro.getCodigo());
		CobroTarjeta cobroTarjeta = new CobroTarjeta();		
		cobroTarjeta.setCodigo(0L);
		cobroTarjeta.setCobro(cobro);
		cobroTarjeta.setNumero(tempCobroTarjeta.getNumero());
		cobroTarjeta.setValor(tempCobroTarjeta.getValor());
		cobroTarjeta.setNumeroVoucher(tempCobroTarjeta.getNumeroVoucher());
		cobroTarjeta.setFechaCaducidad(tempCobroTarjeta.getFechaCaducidad());
		cobroTarjeta.setDetallePlantilla(tempCobroTarjeta.getDetallePlantilla());
		try {
			cobroTarjetaDaoService.save(cobroTarjeta, cobroTarjeta.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en saveCobroEfectivoReal: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.CobroTarjetaService#sumaValorTarjeta(java.lang.Long)
	 */
	public Double sumaValorTarjeta(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo sumaValorTarjeta con id cobro: " + idCobro);
		return cobroTarjetaDaoService.selectSumaByCobro(idCobro);
	}

	@Override
	public CobroTarjeta saveSingle(CobroTarjeta cobroTarjeta) throws Throwable {
		System.out.println("saveSingle - cobroTarjeta");
		cobroTarjeta = cobroTarjetaDaoService.save(cobroTarjeta, cobroTarjeta.getCodigo());
		return cobroTarjeta;
	}
}