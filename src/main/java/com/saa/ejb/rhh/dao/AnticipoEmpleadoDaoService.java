package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.AnticipoEmpleado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService AnticipoEmpleado.
 */
@Local
public interface AnticipoEmpleadoDaoService extends EntityDao<AnticipoEmpleado> {

	/**
	 * Anticipo "vivo" de un empleado: en estado SOLICITADO, APROBADO, PAGADO
	 * o EN_DESCUENTO. Un empleado solo puede tener uno a la vez.
	 * @param idEmpleado : Id del empleado
	 * @return           : El anticipo vivo, o null si no tiene ninguno
	 * @throws Throwable : Excepcion
	 */
	AnticipoEmpleado selectVigenteByEmpleado(Long idEmpleado) throws Throwable;

	/**
	 * Listado de anticipos con filtros opcionales (null = sin filtrar por
	 * ese criterio).
	 * @param idEmpresa  : Empresa del empleado; null = todas
	 * @param idEmpleado : Empleado; null = todos
	 * @param estado     : Estado (rubro 234); null = todos
	 * @return           : Anticipos que cumplen el filtro, mas recientes primero
	 * @throws Throwable : Excepcion
	 */
	List<AnticipoEmpleado> selectListado(Long idEmpresa, Long idEmpleado, Long estado) throws Throwable;

	/**
	 * Recupera el anticipo asociado a un descuento recurrente, para la
	 * integracion inversa desde el rol (T4): dado un DSRCCDGO, saber si
	 * corresponde a un anticipo y cual.
	 * @param idDescuentoRecurrente : Id del DescuentoRecurrente
	 * @return                      : El anticipo, o null si el descuento no viene de uno
	 * @throws Throwable            : Excepcion
	 */
	AnticipoEmpleado selectByDescuentoRecurrente(Long idDescuentoRecurrente) throws Throwable;

}
