/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.ejb.tsr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.CuentaBancariaTitular;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CuentaBancariaTitular.
 * Accede a los métodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface CuentaBancariaTitularService extends EntityService<CuentaBancariaTitular> {

}
