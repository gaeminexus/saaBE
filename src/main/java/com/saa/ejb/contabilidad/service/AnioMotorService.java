/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. J�se Fern�ndez.
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.ejb.contabilidad.service;

import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.AnioMotor;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 *         <p>
 *         Servicio para la entidad AnioMotorService.
 *         Accede a los metodos DAO y procesa los datos para el cliente.
 *         </p>
 */
@Remote
public interface AnioMotorService extends EntityService<AnioMotor> {

	/**
	 * Recupera el listado de anion en forma descendente
	 * 
	 * @return : Listado ordenado descendentemente
	 * @throws Throwable: Excepcion
	 */
	List<AnioMotor> selectOrderDesc() throws Throwable;

}
