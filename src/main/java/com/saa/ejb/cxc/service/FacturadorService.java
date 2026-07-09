package com.saa.ejb.cxc.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cxc.Facturador;

import jakarta.ejb.Local;

/**
 * Interface de servicio para la entidad Facturador
 */
@Local
public interface FacturadorService extends EntityService<Facturador> {
	
	/**
	 * Recupera entidad por ID
	 * @param id ID del facturador
	 * @return Facturador encontrado
	 * @throws Throwable Excepción
	 */
	Facturador selectById(Long id) throws Throwable;
	
	/**
	 * Guarda un facturador
	 * @param facturador Facturador a guardar
	 * @return Facturador guardado
	 * @throws Throwable Excepción
	 */
	Facturador saveSingle(Facturador facturador) throws Throwable;
	
	/**
	 * Busca un facturador por número de documento
	 * @param numDoc Número de documento
	 * @return Facturador encontrado
	 * @throws Throwable Excepción
	 */
	Facturador selectByNumDoc(String numDoc) throws Throwable;
}
