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

	/**
	 * Busca el facturador de una empresa.
	 *
	 * <p>Existe porque el RUC del empleador que exige el archivo de novedades del IESS sale
	 * de aqui: <code>CBR.FCDR</code> es el unico sitio del sistema donde vive el RUC de la
	 * empresa. Se filtra por la FK a <code>PJRQCDGO</code> y no se toma la primera fila de
	 * la tabla, que en una instalacion multiempresa seria la de otra.</p>
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @return				: El facturador de esa empresa, o null
	 * @throws Throwable	: Excepcion
	 */
	Facturador selectByEmpresa(Long idEmpresa) throws Throwable;
}
