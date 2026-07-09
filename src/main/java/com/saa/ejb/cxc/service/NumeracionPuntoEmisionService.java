package com.saa.ejb.cxc.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cxc.NumeracionPuntoEmision;

import jakarta.ejb.Local;

/**
 * Interface de servicio para la entidad NumeracionPuntoEmision
 */
@Local
public interface NumeracionPuntoEmisionService extends EntityService<NumeracionPuntoEmision> {
	
	/**
	 * Recupera entidad por ID
	 * @param id ID de la numeración
	 * @return NumeracionPuntoEmision encontrada
	 * @throws Throwable Excepción
	 */
	NumeracionPuntoEmision selectById(Long id) throws Throwable;
	
	/**
	 * Guarda una numeración
	 * @param numeracion Numeración a guardar
	 * @return Numeración guardada
	 * @throws Throwable Excepción
	 */
	NumeracionPuntoEmision saveSingle(NumeracionPuntoEmision numeracion) throws Throwable;
	
	/**
	 * Busca numeraciones por punto de emisión
	 * @param idPuntoEmision ID del punto de emisión
	 * @return Lista de numeraciones
	 * @throws Throwable Excepción
	 */
	List<NumeracionPuntoEmision> selectByPuntoEmision(Long idPuntoEmision) throws Throwable;
	
	/**
	 * Busca numeración por punto de emisión y tipo de documento
	 * @param idPuntoEmision ID del punto de emisión
	 * @param tipoDoc Tipo de documento
	 * @return Numeración encontrada
	 * @throws Throwable Excepción
	 */
	NumeracionPuntoEmision selectByPuntoEmisionTipo(Long idPuntoEmision, String tipoDoc) throws Throwable;
	
	/**
	 * Obtiene el siguiente número para un tipo de documento
	 * @param idPuntoEmision ID del punto de emisión
	 * @param tipoDoc Tipo de documento
	 * @return Siguiente número disponible
	 * @throws Throwable Excepción
	 */
	Long obtenerSiguienteNumero(Long idPuntoEmision, String tipoDoc) throws Throwable;
}
