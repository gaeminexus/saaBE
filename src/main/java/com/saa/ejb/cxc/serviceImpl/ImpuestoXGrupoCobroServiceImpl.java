/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.ImpuestoXGrupoCobroDaoService;
import com.saa.ejb.cxc.service.ImpuestoXGrupoCobroService;
import com.saa.model.cxc.ImpuestoXGrupoCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ImpuestoXGrupoCobroService.
 *  Contiene los servicios relacionados con la entidad ImpuestoXGrupoCobro</p>
 */
@Stateless
public class ImpuestoXGrupoCobroServiceImpl implements ImpuestoXGrupoCobroService {
	
	@EJB
	private ImpuestoXGrupoCobroDaoService impuestoXGrupoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<ImpuestoXGrupoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de impuestoXGrupoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (ImpuestoXGrupoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			impuestoXGrupoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de impuestoXGrupoCobro service");
		//INSTANCIA UNA ENTIDAD
		ImpuestoXGrupoCobro impuestoXGrupoCobro = new ImpuestoXGrupoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			impuestoXGrupoCobroDaoService.remove(impuestoXGrupoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<ImpuestoXGrupoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll ImpuestoXGrupoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ImpuestoXGrupoCobro> result = impuestoXGrupoCobroDaoService.selectAll(NombreEntidadesCobro.IMPUESTO_X_GRUPO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ImpuestoXGrupoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ImpuestoXGrupoCobroService#selectById(java.lang.Long)
	 */
	public ImpuestoXGrupoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return impuestoXGrupoCobroDaoService.selectById(id, NombreEntidadesCobro.IMPUESTO_X_GRUPO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ImpuestoXGrupoCobroService#selectByCriteria(java.util.List)
	 */
	public List<ImpuestoXGrupoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria ImpuestoXGrupoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ImpuestoXGrupoCobro> result = impuestoXGrupoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.IMPUESTO_X_GRUPO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ImpuestoXGrupoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de ImpuestoXGrupoCobro.
	 */
	@Override
	public ImpuestoXGrupoCobro saveSingle(ImpuestoXGrupoCobro impuestoXGrupoCobro) throws Throwable {
		System.out.println("saveSingle - ImpuestoXGrupoCobroService");
		impuestoXGrupoCobro = impuestoXGrupoCobroDaoService.save(impuestoXGrupoCobro, impuestoXGrupoCobro.getCodigo());
		return impuestoXGrupoCobro;
	}
}