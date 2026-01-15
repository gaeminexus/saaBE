/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AnexoContratoDaoService;
import com.saa.ejb.rhh.service.AnexoContratoService;
import com.saa.model.rhh.AnexoContrato;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz AnexoContratoService.
 *  Contiene los servicios relacionados con la entidad AnexoContrato</p>
 */
@Stateless
public class AnexoContratoServiceImpl implements AnexoContratoService {
	
	@EJB
	private AnexoContratoDaoService anexoContratoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<AnexoContrato> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de anexoContrato service");
		for (AnexoContrato registro:lista) {			
			anexoContratoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de anexoContrato service");
		//INSTANCIA UNA ENTIDAD
		AnexoContrato anexoContrato = new AnexoContrato();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				anexoContratoDaoService.remove(anexoContrato, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<AnexoContrato> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) AnexoContrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AnexoContrato> result = anexoContratoDaoService.selectAll(NombreEntidadesRhh.ANEXO_CONTRATO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de anexoContrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.AnexoContratoService#selectById(java.lang.Long)
	 */
	public AnexoContrato selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return anexoContratoDaoService.selectById(id, NombreEntidadesRhh.ANEXO_CONTRATO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.AnexoContratoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<AnexoContrato> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AnexoContrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AnexoContrato> result = anexoContratoDaoService.selectByCriteria(datos, NombreEntidadesRhh.ANEXO_CONTRATO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de anexoContrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public AnexoContrato saveSingle(AnexoContrato anexoContrato) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) AnexoContrato");
		anexoContrato = anexoContratoDaoService.save(anexoContrato, anexoContrato.getCodigo());
		return anexoContrato;
	}
}