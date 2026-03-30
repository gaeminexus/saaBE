package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.HistoricoDesgloseAporteParticipe;

import jakarta.ejb.Local;

@Local
public interface HistoricoDesgloseAporteParticipeDaoService extends EntityDao<HistoricoDesgloseAporteParticipe> {
	
	/**
	 * Busca todos los registros de desglose por ID de carga
	 * 
	 * @param idCarga ID de la carga
	 * @return Lista de registros encontrados
	 * @throws Throwable Si ocurre algún error
	 */
	List<HistoricoDesgloseAporteParticipe> selectByCarga(Long idCarga) throws Throwable;
	
	/**
	 * Busca registros de desglose por cédula del partícipe
	 * 
	 * @param cedula Número de cédula
	 * @return Lista de registros encontrados
	 * @throws Throwable Si ocurre algún error
	 */
	List<HistoricoDesgloseAporteParticipe> selectByCedula(String cedula) throws Throwable;
	
	/**
	 * Busca un registro específico por código interno y carga
	 * 
	 * @param codigoInterno Código interno del partícipe en Petrocomercial
	 * @param idCarga ID de la carga
	 * @return Registro encontrado o null
	 * @throws Throwable Si ocurre algún error
	 */
	HistoricoDesgloseAporteParticipe selectByCodigoInternoYCarga(String codigoInterno, Long idCarga) throws Throwable;
}
