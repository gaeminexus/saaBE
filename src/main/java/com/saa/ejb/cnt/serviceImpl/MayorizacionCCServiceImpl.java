package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.MayorizacionCCDaoService;
import com.saa.ejb.cnt.service.DetalleMayorizacionCCService;
import com.saa.ejb.cnt.service.MayorizacionCCService;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.MayorizacionCC;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.Periodo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz MayorizacionCCService.
 *  Contiene los servicios relacionados con la entidad MayorizacionCC.</p>
 */
@Stateless
public class MayorizacionCCServiceImpl implements MayorizacionCCService {
	
	
	@EJB
	private MayorizacionCCDaoService mayorizacionCCDaoService;		
		
	@EJB
	private DetalleMayorizacionCCService detalleMayorizacionCCService;	
	
	@EJB
	private DetalleRubroService detalleRubroService;	

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
	    System.out.println("Ingresa al metodo remove[] de MayorizacionCC service ... depurado");
	    // INSTANCIA LA ENTIDAD
	    MayorizacionCC mayorizacionCC = new MayorizacionCC();
	    // ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
	    for (Long registro : id) {
	        mayorizacionCCDaoService.remove(mayorizacionCC, registro);
	    }
	}
				

	
	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<MayorizacionCC>)
	 */
	public void save(List<MayorizacionCC> lista) throws Throwable {
	    System.out.println("Ingresa al metodo save de mayorizacionCC service");
	    // BARRIDA COMPLETA DE LOS REGISTROS
	    for (MayorizacionCC mayorizacionCC : lista) {            
	        // INSERTA O ACTUALIZA REGISTRO
	        mayorizacionCCDaoService.save(mayorizacionCC, mayorizacionCC.getCodigo());
	    }
	}


	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<MayorizacionCC> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo selectAll MayorizacionCCService");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<MayorizacionCC> result = mayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.MAYORIZACION_CC); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda total MayorizacionCC no devolvio ningun registro");
	    }
	    return result;
	}

	/* (non-Javadoc) 
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<MayorizacionCC> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) MayorizacionCC");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<MayorizacionCC> result = mayorizacionCCDaoService.selectByCriteria(
	        datos, NombreEntidadesContabilidad.MAYORIZACION_CC
	    ); 
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        throw new IncomeException("Busqueda por criterio de MayorizacionCC no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionCCService#selectById(java.lang.Long)
	 */
	public MayorizacionCC selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return mayorizacionCCDaoService.selectById(id, NombreEntidadesContabilidad.MAYORIZACION_CC);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionCCService#mayorizacionCC(com.compuseg.income.contabilidad.ejb.model.Mayorizacion, com.compuseg.income.contabilidad.ejb.model.Periodo)
	 */
	public void mayorizacionCC(Mayorizacion mayorizacion, Periodo periodo, Mayorizacion maximaAnterior) throws Throwable {
		System.out.println("Ingresa al mayorizacionCC con mayorizacion: " + mayorizacion.getCodigo() + ", en periodo: " + periodo.getCodigo());		
		// GENERA DATOS DE MAYORIZACION
		MayorizacionCC mayorizacionCCGenerada = generaDatosMayorizacionCC(mayorizacion, periodo, maximaAnterior);
		// GENERA SALDOS
		generaSaldosByCC(mayorizacionCCGenerada.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionCCService#save(com.compuseg.income.contabilidad.ejb.model.MayorizacionCC)
	 */
	public void save(MayorizacionCC mayorizacionCC) throws Throwable {
		System.out.println("Ingresa al save con id: " + mayorizacionCC.getCodigo());
		mayorizacionCCDaoService.save(mayorizacionCC, mayorizacionCC.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionCCService#creaByMayorizacion(java.lang.Long, com.compuseg.income.contabilidad.ejb.model.Periodo)
	 */
	public void creaByMayorizacion(Long idMayorizacion, Periodo periodo) throws Throwable {
		System.out.println("Ingresa al creaByMayorizacion con id: " + idMayorizacion);
		MayorizacionCC mayorizacionCC = new MayorizacionCC();
		mayorizacionCC.setCodigo(idMayorizacion);
		mayorizacionCC.setPeriodo(periodo);
		mayorizacionCC.setFecha(LocalDateTime.now());
		mayorizacionCCDaoService.save(mayorizacionCC, mayorizacionCC.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionCCService#generaDatosMayorizacionCC(com.compuseg.income.contabilidad.ejb.model.Mayorizacion, com.compuseg.income.contabilidad.ejb.model.Periodo)
	 */
	public MayorizacionCC generaDatosMayorizacionCC(Mayorizacion mayorizacion, Periodo periodo, Mayorizacion maximaAnterior) throws Throwable {
		System.out.println("Ingresa al generaDatosMayorizacionCC con id: " + mayorizacion.getCodigo() + ", en periodo: " + periodo.getCodigo());
		creaByMayorizacion(mayorizacion.getCodigo(), periodo);
		MayorizacionCC ingresada = selectById(mayorizacion.getCodigo());
		detalleMayorizacionCCService.creaDetalleMayorizacionCC(ingresada, periodo.getEmpresa().getCodigo());
		detalleMayorizacionCCService.creaDesgloseDetalle(ingresada.getCodigo(), periodo.getEmpresa().getCodigo());
		detalleMayorizacionCCService.creaSaldoInicial(ingresada, maximaAnterior);
		return ingresada;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionCCService#generaSaldosByCC(com.compuseg.income.contabilidad.ejb.model.MayorizacionCC, com.compuseg.income.contabilidad.ejb.model.Periodo)
	 */
	public void generaSaldosByCC(Long mayorizacionCC) throws Throwable {
		System.out.println("Ingresa al generaSaldosByCC con id: " + mayorizacionCC);
		detalleMayorizacionCCService.generaSaldosDesglose(mayorizacionCC);
		detalleMayorizacionCCService.generaSaldosDesglosePadres(mayorizacionCC);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.MayorizacionCCService#eliminaByMayorizacionCC(java.lang.Long)
	 */
	public void eliminaByMayorizacionCC(Long mayorizacionCC) throws Throwable {
		System.out.println("Ingresa al eliminaByMayorizacionCC con id: " + mayorizacionCC);
		detalleMayorizacionCCService.eliminaDetalleByMayorizacionCC(mayorizacionCC);
		mayorizacionCCDaoService.deleteByMayorizacionCC(mayorizacionCC);
	}


	@Override
	public MayorizacionCC saveSingle(MayorizacionCC mayorizacionCC) throws Throwable {
	    System.out.println("saveSingle - MayorizacionCCService");
	    mayorizacionCC = mayorizacionCCDaoService.save(mayorizacionCC, mayorizacionCC.getCodigo());
	    return mayorizacionCC;
	}

	
}
