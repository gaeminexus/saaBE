/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.AuxDepositoBancoDaoService;
import com.saa.ejb.tesoreria.service.AuxDepositoBancoService;
import com.saa.model.tesoreria.AuxDepositoBanco;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz AuxDepositoBancoService.
 *  Contiene los servicios relacionados con la entidad AuxDepositoBanco.</p>
 */
@Stateless
public class AuxDepositoBancoServiceImpl implements AuxDepositoBancoService {
	
	@EJB
	private AuxDepositoBancoDaoService auxDepositoBancoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoBancoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
	    System.out.println("Ingresa al metodo remove[] de AuxDepositoBanco service ... depurado");
	    // INSTANCIA LA ENTIDAD
	    AuxDepositoBanco auxDepositoBanco = new AuxDepositoBanco();
	    // ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
	    for (Long registro : id) {
	        auxDepositoBancoDaoService.remove(auxDepositoBanco, registro);
	    }
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoBancoService#save(java.lang.Object[][])
	 */
	public void save(List<AuxDepositoBanco> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de auxDepositoBanco service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (AuxDepositoBanco registro : lista) {			
			auxDepositoBancoDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoBancoService#selectAll()
	 */
	public List<AuxDepositoBanco> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) auxDepositoBanco Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AuxDepositoBanco> result = auxDepositoBancoDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_BANCO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total auxDepositoBanco no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<AuxDepositoBanco> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AuxDepositoBanco");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AuxDepositoBanco> result = auxDepositoBancoDaoService.selectByCriteria(datos, NombreEntidadesTesoreria.AUX_DEPOSITO_BANCO); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total auxDepositoBanco no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoBancoService#selectById(java.lang.Long)
	 */
	public AuxDepositoBanco selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return auxDepositoBancoDaoService.selectById(id, NombreEntidadesTesoreria.AUX_DEPOSITO_BANCO);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoBancoService#selectIdByUsuarioCajaBancoCuenta(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public AuxDepositoBanco selectIdByUsuarioCajaBancoCuenta(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectIdByUsuarioCajaBancoCuenta con id de usuario por caja: " + idUsuarioCaja + " id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		return auxDepositoBancoDaoService.selectIdByUsuarioCajaBancoCuenta(idUsuarioCaja, idBanco, idCuenta);
	}

	public void actualizaSaldosCuenta(AuxDepositoBanco auxDepositoBanco, Double efectivo, Double cheque) throws Throwable {
		System.out.println("Ingresa al metodo actualizaSaldosCuenta con id: " + auxDepositoBanco.getCodigo() + ", valor efectivo: " + efectivo + " y valor cheque: " + cheque);
		auxDepositoBanco.setValor(efectivo + cheque);
		auxDepositoBanco.setValorEfectivo(efectivo);
		auxDepositoBanco.setValorCheque(cheque);
		try {
			auxDepositoBancoDaoService.save(auxDepositoBanco, auxDepositoBanco.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en el metodo actualizaSaldosCuenta: " + e.getCause());			
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoBancoService#selectByUsuarioCaja(java.lang.Long)
	 */
	public List<AuxDepositoBanco> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectByUsuarioCaja con id de usuario por caja: " + idUsuarioCaja);
		return auxDepositoBancoDaoService.selectByUsuarioCaja(idUsuarioCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoBancoService#eliminaPorUsuarioCaja(java.lang.Long)
	 */
	public void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaPorUsuarioCaja con idUsuarioCaja: " + idUsuarioCaja);
		auxDepositoBancoDaoService.eliminaPorUsuarioCaja(idUsuarioCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoBancoService#eliminaPorUsuarioCajaCuenta(java.lang.Long, java.lang.Long)
	 */
	public void eliminaPorUsuarioCajaCuenta(Long idUsuarioCaja, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaPorUsuarioCaja con idUsuarioCaja: " + idUsuarioCaja+", id cuenta: "+idCuenta);
		auxDepositoBancoDaoService.eliminaPorUsuarioCajaCuenta(idUsuarioCaja, idCuenta);
	}

	@Override
	public AuxDepositoBanco saveSingle(AuxDepositoBanco auxDepositoBanco) throws Throwable {
		System.out.println("saveSingle - AuxDepositoBanco");
		auxDepositoBanco = auxDepositoBancoDaoService.save(auxDepositoBanco, auxDepositoBanco.getCodigo());
		return auxDepositoBanco;
	}
}
