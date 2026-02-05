package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.AuxDepositoCierreDaoService;
import com.saa.ejb.tsr.service.AuxDepositoCierreService;
import com.saa.ejb.tsr.service.CierreCajaService;
import com.saa.ejb.tsr.service.UsuarioPorCajaService;
import com.saa.model.tsr.AuxDepositoCierre;
import com.saa.model.tsr.CierreCaja;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.UsuarioPorCaja;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz AuxDepositoCierreService.
 *  Contiene los servicios relacionados con la entidad AuxDepositoCierre.</p>
 */
@Stateless
public class AuxDepositoCierreServiceImpl implements AuxDepositoCierreService {
	
	@EJB
	private AuxDepositoCierreDaoService auxDepositoCierreDaoService;
	
	@EJB
	private CierreCajaService cierreCajaService;
	
	@EJB
	private UsuarioPorCajaService usuarioPorCajaService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoCierreService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de AuxDepositoCierre service ... depurado");
	    // INSTANCIA LA ENTIDAD
		AuxDepositoCierre auxDepositoCierre = new AuxDepositoCierre();
	    // ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
	    for (Long registro : id) {
	    	auxDepositoCierreDaoService.remove(auxDepositoCierre, registro);
	    }	
	}				
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoCierreService#save(java.lang.Object[][])
	 */
	public void save(List<AuxDepositoCierre> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de AuxDepositoCierre service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (AuxDepositoCierre registro : lista) {			
			auxDepositoCierreDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoCierreService#selectAll()
	 */
	public List<AuxDepositoCierre> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) auxDepositoCierre Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AuxDepositoCierre> result = auxDepositoCierreDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_CIERRE); 
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
	public List<AuxDepositoCierre> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AuxDepositoCierre");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AuxDepositoCierre> result = auxDepositoCierreDaoService.selectByCriteria(datos, NombreEntidadesTesoreria.AUX_DEPOSITO_CIERRE); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total auxDepositoBanco no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoCierreService#selectById(java.lang.Long)
	 */
	public AuxDepositoCierre selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return auxDepositoCierreDaoService.selectById(id, NombreEntidadesTesoreria.AUX_DEPOSITO_CIERRE);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoCierreService#selectByUsuarioCaja(java.lang.Long)
	 */
	public List<AuxDepositoCierre> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo selectByUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja );
		return auxDepositoCierreDaoService.selectByUsuarioCaja(idUsuarioCaja);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoCierreService#eliminaPorUsuarioCaja(java.lang.Long)
	 */
	public void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaPorUsuarioCaja con idUsuarioCaja: " + idUsuarioCaja);
		auxDepositoCierreDaoService.eliminaPorUsuarioCaja(idUsuarioCaja);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.AuxDepositoCierreService#insertarCierresPendientes(java.lang.Long)
	 */
	public void insertarCierresPendientes(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo insertarCierresPendientes con idUsuarioCaja: " + idUsuarioCaja);
		AuxDepositoCierre auxDepositoCierre = new AuxDepositoCierre();
		List<CierreCaja> listaCierres = cierreCajaService.selectByIdDeposito(idUsuarioCaja);
		UsuarioPorCaja usuarioPorCaja = usuarioPorCajaService.selectById(idUsuarioCaja);
		if(listaCierres.isEmpty())
			throw new IncomeException("NO EXISTE CIERRES PARA ESTA CAJA");
		else{
			for(CierreCaja cierreCaja : listaCierres){
				auxDepositoCierre.setCodigo(0L);
				auxDepositoCierre.setCierreCaja(cierreCaja);
				auxDepositoCierre.setUsuarioPorCaja(usuarioPorCaja);
				Double montoEfectivo = 0.0;
				if(cierreCaja.getMontoEfectivo() != null){
					montoEfectivo = cierreCaja.getMontoEfectivo();
				}
				auxDepositoCierre.setMontoEfectivo(montoEfectivo);				
				Double montoCheque = 0.0;
				if(cierreCaja.getMontoCheque() != null){
					montoCheque = cierreCaja.getMontoCheque();
				}
				auxDepositoCierre.setMontoCheque(montoCheque);
				auxDepositoCierre.setSeleccionado(0L);
				Double montoDeposito = montoEfectivo+montoCheque;
				auxDepositoCierre.setMontoDeposito(montoDeposito);
				auxDepositoCierre.setMontoTotalCierre(cierreCaja.getMonto());
				auxDepositoCierre.setFechaCierre(cierreCaja.getFechaCierre());
				auxDepositoCierre.setNombreCaja(usuarioPorCaja.getCajaFisica().getNombre());
				try {
					auxDepositoCierreDaoService.save(auxDepositoCierre, auxDepositoCierre.getCodigo());
				} catch (EJBException e) {
					throw new IncomeException("ERROR AL INSERTAR AUXILIAR DE DEPOSITO CIERRE: "+e.getCause());
				}
			}
		}
	}

	@Override
	public AuxDepositoCierre saveSingle(AuxDepositoCierre auxDepositoCierre) throws Throwable {
		System.out.println("saveSingle - AuxDepositoCierre");
		auxDepositoCierre = auxDepositoCierreDaoService.save(auxDepositoCierre, auxDepositoCierre.getCodigo());
		return auxDepositoCierre;
	}

}
