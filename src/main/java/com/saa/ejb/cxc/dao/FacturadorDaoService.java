package com.saa.ejb.cxc.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.Facturador;

import jakarta.ejb.Local;

/**
 * Interface DAO para la entidad Facturador
 */
@Local
public interface FacturadorDaoService extends EntityDao<Facturador> {
	
	/**
	 * Busca un facturador por número de documento
	 * @param numDoc Número de documento (RUC/Cédula)
	 * @return Facturador encontrado
	 * @throws Throwable Excepción
	 */
	Facturador selectByNumDoc(String numDoc) throws Throwable;
}
