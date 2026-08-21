/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.Marcaciones;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService Marcaciones. 
 */
@Local
public interface MarcacionesDaoService  extends EntityDao<Marcaciones>  {
	
	/**
	 * Indica si ya existe una marcacion del empleado en ese instante exacto.
	 *
	 * <p>Es la deduplicacion de la regla 6: los relojes repiten marcaciones cuando alguien
	 * pasa el dedo dos veces, y el archivo del mes siguiente suele traer solapado el final
	 * del anterior.</p>
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param fechaHora		: Instante de la marcacion
	 * @return				: true si ya existe
	 * @throws Throwable	: Excepcion
	 */
	boolean existeMarcacion(Long idEmpleado, LocalDateTime fechaHora) throws Throwable;

	/**
	 * Recupera las marcaciones de un empleado en un dia, ordenadas por hora.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param dia			: Dia a consultar
	 * @return				: Marcaciones del dia
	 * @throws Throwable	: Excepcion
	 */
	List<Marcaciones> selectByEmpleadoYDia(Long idEmpleado, LocalDate dia) throws Throwable;

	/**
	 * Recupera las marcaciones sin consolidar de un rango, ordenadas por empleado y hora.
	 *
	 * @param desde			: Fecha desde
	 * @param hasta			: Fecha hasta
	 * @return				: Marcaciones pendientes de consolidar
	 * @throws Throwable	: Excepcion
	 */
	List<Marcaciones> selectPendientesConsolidar(LocalDate desde, LocalDate hasta) throws Throwable;

	/**
	 * Elimina las marcaciones de un lote. Lo usa la anulacion de la carga.
	 *
	 * @param idCarga		: Id de la carga
	 * @return				: Numero de marcaciones eliminadas
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByCarga(Long idCarga) throws Throwable;

}
