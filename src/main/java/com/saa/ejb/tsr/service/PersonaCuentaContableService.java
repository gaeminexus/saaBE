package com.saa.ejb.tsr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.PersonaCuentaContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad PersonaCuentaContable.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface PersonaCuentaContableService extends EntityService<PersonaCuentaContable>{
	
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
}
