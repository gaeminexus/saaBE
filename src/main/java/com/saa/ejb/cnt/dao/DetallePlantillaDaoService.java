/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.cnt.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad DetallePlantilla.
 */
@Local
public interface DetallePlantillaDaoService extends EntityDao<DetallePlantilla> {

	/**
	 * Recupera el detalle de plantilla para el cierre anual.
	 * @param plantilla	: Id de la plantilla  
	 * @return			: Listado de detalle plantilla
	 * @throws Throwable: Excepcion
	 */
	List<DetallePlantilla> selectByPlantilla(Long plantilla) throws Throwable;

	/**
	 * Recupera la cuenta contable del detalle de plantilla
	 * @param idDetallePlantilla: Id del detalle de plantilla
	 * @return					: Cuenta contable recuperada
	 * @throws Throwable		: Excepcion
	 */
	PlanCuenta recuperaCuentaContable(Long idDetallePlantilla) throws Throwable;
	
	/**
	 * Recupera el detalle de mayorizacion relacionado a una cuenta
	 * @param idPlanCuenta	:Id de la cuenta
	 * @return				:Detalle de mayorizacion
	 * @throws Throwable	:Excepcion
	 */
	List<DetallePlantilla> selectByIdPlanCuenta(Long idPlanCuenta) throws Throwable;

	/**
	 * Recupera la linea de una plantilla identificada por su auxiliar1.
	 *
	 * <p>RHH estrena el uso de DTPLAXL1: lleva el codigo alterno del detalle del rubro 214
	 * RHH_LINEA_ASIENTO, que dice que papel cumple esa linea dentro del asiento. Solo
	 * considera lineas activas.</p>
	 *
	 * @param idPlantilla	: Id de la plantilla (CNT.PLNS)
	 * @param auxiliar1		: Codigo alterno del detalle del rubro 214
	 * @return				: La linea, o null si la plantilla no la define
	 * @throws Throwable	: Excepcion
	 */
	DetallePlantilla selectByPlantillaYAuxiliar(Long idPlantilla, int auxiliar1) throws Throwable;

	/**
	 * Recupera la linea de una plantilla identificada por su auxiliar1 MAS una segunda
	 * dimension en auxiliar2.
	 *
	 * <p>Lo estrena el cierre de cartera de CRD: el papel de la linea (auxiliar1, el rubro
	 * CRD_LINEA_ASIENTO) no basta cuando la cuenta cambia ademas por familia de producto
	 * --el interes ordinario va a 1.4.02.05 en quirografario, .10 en prendario y .15 en
	 * hipotecario--. En ese caso auxiliar2 lleva el CRD.TPPR.TPPRCDGO y la plantilla define
	 * una linea por combinacion. Asi la parametrizacion queda en la base y no en el codigo.</p>
	 *
	 * <p>Solo considera lineas activas.</p>
	 *
	 * @param idPlantilla	: Id de la plantilla (CNT.PLNS)
	 * @param auxiliar1		: Codigo del papel de la linea
	 * @param auxiliar2		: Segunda dimension; en CRD, el tipo de prestamo
	 * @return				: La linea, o null si la plantilla no define esa combinacion
	 * @throws Throwable	: Excepcion
	 */
	DetallePlantilla selectByPlantillaYAuxiliares(Long idPlantilla, int auxiliar1, int auxiliar2)
			throws Throwable;

}
