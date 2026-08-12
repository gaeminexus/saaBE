/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.PersonaCuentaContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice PersonaCuentaContable.  
 */
@Local
public interface PersonaCuentaContableDaoService extends EntityDao<PersonaCuentaContable> {

	/**
	 * Recupera las cuentas por codigo de persona
	 * @param idEmpresa		: Id de empresa
	 * @param idPersona		: Id de persona
	 * @param rolPersona	: 1 = cliente, 2 = proveedor 
	 * @param tipoCuenta	: tipos de cuenta
	 * @return				: Listado de cuentas por persona
	 * @throws Throwable	: Excepcion
	 */
	List<PersonaCuentaContable> selectByPersonaTipoCuenta(Long idEmpresa, Long idPersona, int rolPersona, Long tipoCuenta) throws Throwable;

	/**
	 * Recupera las cuentas contables de un titular para un ROL concreto,
	 * resolviendo el rol por {@code PersonaRol.rubroRolPersonaH} en vez de por
	 * {@code PersonaCuentaContable.tipoPersona} (ese campo está null en todos
	 * los registros de la BD).
	 * <p>
	 * El filtro por rol es imprescindible: un mismo titular puede ser cliente Y
	 * proveedor a la vez, y entonces tiene dos cuentas con el mismo tipoCuenta
	 * y la misma empresa. Sin filtrar por rol se devuelve una fila arbitraria y
	 * se termina usando la cuenta del rol equivocado.
	 * <p>
	 * Ojo con el sufijo del campo: la convención es {@code ...P} = alterno del
	 * rubro PADRE ({@code Rubros.ROL_PERSONA} = 55) y {@code ...H} = alterno del
	 * detalle HIJO (1=Cliente, 2=Proveedor). El que discrimina es {@code ...H}.
	 * <p>
	 * Si el titular no tiene ninguna cuenta bajo ese rol, reintenta sin el
	 * filtro de rol por compatibilidad con datos antiguos donde
	 * {@code rubroRolPersonaH} no está poblado. Ese reintento nunca puede
	 * devolver la cuenta del otro rol cuando ambos existen, porque sólo se
	 * ejecuta cuando la primera consulta no devolvió nada.
	 *
	 * @param idEmpresa    : Id de la empresa contable
	 * @param codigoTitular: Codigo del titular
	 * @param rolPersona   : {@code RolPersona.CLIENTE} (1) o {@code RolPersona.PROVEEDOR} (2)
	 * @param tipoCuenta   : 1 = Facturas, 2 = Anticipos, 3 = Caja/Banco
	 * @return             : Cuentas del titular para ese rol y tipo (lista vacia si no hay)
	 * @throws Throwable   : Excepcion
	 */
	List<PersonaCuentaContable> selectByTitularRolTipoCuenta(Long idEmpresa, Long codigoTitular,
			int rolPersona, Long tipoCuenta) throws Throwable;

}
