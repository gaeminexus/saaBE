/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.ejb.tsr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.CuentaBancariaTitularDaoService;
import com.saa.model.tsr.CuentaBancariaTitular;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 *
 * Implementacion CuentaBancariaTitularDaoService.
 */
@Stateless
public class CuentaBancariaTitularDaoServiceImpl extends EntityDaoImpl<CuentaBancariaTitular>
        implements CuentaBancariaTitularDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) CuentaBancariaTitular");
        return new String[]{
            "codigo",
            "titular",
            "banco",
            "tipoCuenta",
            "numeroCuenta",
            "observaciones",
            "estado",
            "fechaCreacion",
            "usuarioCreacion"
        };
    }
}
