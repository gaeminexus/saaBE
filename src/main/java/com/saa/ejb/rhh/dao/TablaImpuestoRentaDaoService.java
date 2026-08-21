package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.TablaImpuestoRenta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService TablaImpuestoRenta.
 */
@Local
public interface TablaImpuestoRentaDaoService extends EntityDao<TablaImpuestoRenta> {


	/**
	 * Localiza el tramo de la tabla del impuesto a la renta que contiene una base:
	 * TBIRFRBS menor o igual a la base, y TBIREXCS nulo o mayor que la base.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio fiscal
	 * @param base			: Base imponible
	 * @return				: El tramo aplicable, o null si el anio no tiene tabla
	 * @throws Throwable	: Excepcion
	 */
	TablaImpuestoRenta selectTramo(Long idEmpresa, Integer anio, Double base) throws Throwable;

	/**
	 * Recupera la tabla completa de un anio, ordenada por tramo.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio fiscal
	 * @return				: Listado de tramos; vacio si el anio no tiene tabla
	 * @throws Throwable	: Excepcion
	 */
	List<TablaImpuestoRenta> selectByAnio(Long idEmpresa, Integer anio) throws Throwable;
}
