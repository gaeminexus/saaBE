/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.NaturalezaCuenta;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad NaturalezaCuenta.
 */
@Remote
public interface NaturalezaCuentaDaoService extends EntityDao<NaturalezaCuenta> {
	
	/**
	 * Elimina las naturalezas de cuenta de una empresa
	 * @param empresa		:Id de la empresa de la que se eliminara las naturalezas de cuenta
	 * @throws Throwable	:Excepcion
	 */
	void deleteByEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Recupera la naturaleza de cuenta por numero y empresa que se encuetre activa
	 * @param numero	: Numero de grupo
	 * @param empresa	: Empresa
	 * @return			: Registro recuperado
	 * @throws Throwable: Excepcion
	 */
	NaturalezaCuenta selectByNumeroEmpresa(Long numero, Long empresa) throws Throwable;
	
	/**
	 * Recupera la naturaleza de cuenta por numero y empresa que se encuetre activa sin manejo de excepciones
	 * @param numero	: Numero de grupo
	 * @param empresa	: Empresa
	 * @return			: Registro recuperados en listado
	 * @throws Throwable: Excepcion
	 */
	List<NaturalezaCuenta> selectByNumeroEmpresaSinExce(String numero, Long empresa) throws Throwable;
	
	/**
	 * Recupera todas las naturaleza de cuenta que pertenecen a una empresa
	 * @param empresa	: Empresa
	 * @return			: Naturalezas que pertenecen a una empresa
	 * @throws Throwable: Excepcion
	 */
	List<NaturalezaCuenta> selectByEmpresa(Long empresa) throws Throwable;

	/**
	 * Recupera todas las naturaleza de cuenta activas que pertenecen a una empresa
	 * @param empresa	: Empresa
	 * @return			: Naturalezas activas que pertenecen a una empresa
	 * @throws Throwable: Excepcion
	 */
	List<NaturalezaCuenta> selectActivosByEmpresa(Long empresa) throws Throwable;
	
}