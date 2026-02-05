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
import com.saa.ejb.tesoreria.dao.MotivoCobroDaoService;
import com.saa.ejb.tesoreria.service.MotivoCobroService;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.MotivoCobro;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempMotivoCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz MotivoCobroService.
 *  Contiene los servicios relacionados con la entidad MotivoCobro.</p>
 */
@Stateless
public class MotivoCobroServiceImpl implements MotivoCobroService {
	
	@EJB
	private MotivoCobroDaoService motivoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de motivoCobro service");
		MotivoCobro motivoCobro = new MotivoCobro();
		for (Long registro : id) {
			motivoCobroDaoService.remove(motivoCobro, registro);	
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.List<MotivoCobro>)
	 */
	public void save(List<MotivoCobro> list) throws Throwable {
		System.out.println("Ingresa al metodo save de motivoCobro service");
		for (MotivoCobro registro : list) {			
			motivoCobroDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	public List<MotivoCobro> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) motivoCobro Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<MotivoCobro> result = motivoCobroDaoService.selectAll(NombreEntidadesTesoreria.MOTIVO_COBRO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total MotivoCobro no devolvio ningun registro");
		}	
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<MotivoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) MotivoCobro");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<MotivoCobro> result = motivoCobroDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.MOTIVO_COBRO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total MotivoCobro no devolvio ningun registro");
		}	
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MotivoCobroService#selectById(java.lang.Long)
	 */
	public MotivoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return motivoCobroDaoService.selectById(id, NombreEntidadesTesoreria.MOTIVO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.MotivoCobroService#saveMotivoCobroReal(com.compuseg.income.tesoreria.ejb.model.TempMotivoCobro, com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public void saveMotivoCobroReal(TempMotivoCobro tempMotivoCobro, Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo saveMotivoCobroReal con idCobro: " + cobro.getCodigo());
		MotivoCobro motivoCobro = new MotivoCobro();
		motivoCobro.setCodigo(0L);
		motivoCobro.setCobro(cobro);
		motivoCobro.setDescripcion(tempMotivoCobro.getDescripcion());
		motivoCobro.setValor(tempMotivoCobro.getValor());
		motivoCobro.setDetallePlantilla(tempMotivoCobro.getDetallePlantilla());
		try {
			motivoCobroDaoService.save(motivoCobro, motivoCobro.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("Error en saveMotivoCobroReal: " + e.getCause());
		}
	}

	@Override
	public MotivoCobro saveSingle(MotivoCobro motivoCobro) throws Throwable {
		System.out.println("saveSingle - MotivoCobro");
		motivoCobro = motivoCobroDaoService.save(motivoCobro, motivoCobro.getCodigo());
		return motivoCobro;
	}
}
