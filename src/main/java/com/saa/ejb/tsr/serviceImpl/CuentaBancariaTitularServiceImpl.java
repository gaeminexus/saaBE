/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.CuentaBancariaTitularDaoService;
import com.saa.ejb.tsr.service.CuentaBancariaTitularService;
import com.saa.model.tsr.CuentaBancariaTitular;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CuentaBancariaTitularService.
 * Contiene los servicios relacionados con la entidad CuentaBancariaTitular.</p>
 */
@Stateless
public class CuentaBancariaTitularServiceImpl implements CuentaBancariaTitularService {

    @EJB
    private CuentaBancariaTitularDaoService cuentaBancariaTitularDaoService;

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CuentaBancariaTitular service");
        CuentaBancariaTitular cuentaBancariaTitular = new CuentaBancariaTitular();
        for (Long registro : id) {
            cuentaBancariaTitularDaoService.remove(cuentaBancariaTitular, registro);
        }
    }

    @Override
    public void save(List<CuentaBancariaTitular> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CuentaBancariaTitular service");
        for (CuentaBancariaTitular registro : lista) {
            cuentaBancariaTitularDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CuentaBancariaTitular> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo (selectAll) CuentaBancariaTitularService");
        List<CuentaBancariaTitular> result = cuentaBancariaTitularDaoService
                .selectAll(NombreEntidadesTesoreria.CUENTA_BANCARIA_TITULAR);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CuentaBancariaTitular no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<CuentaBancariaTitular> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) CuentaBancariaTitular");
        List<CuentaBancariaTitular> result = cuentaBancariaTitularDaoService
                .selectByCriteria(datos, NombreEntidadesTesoreria.CUENTA_BANCARIA_TITULAR);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda selectByCriteria CuentaBancariaTitular no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CuentaBancariaTitular selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CuentaBancariaTitular con id: " + id);
        return cuentaBancariaTitularDaoService.selectById(id, NombreEntidadesTesoreria.CUENTA_BANCARIA_TITULAR);
    }

    @Override
    public CuentaBancariaTitular saveSingle(CuentaBancariaTitular cuentaBancariaTitular) throws Throwable {
        System.out.println("saveSingle - CuentaBancariaTitular");
        // Si codigo llega como 0 desde el cliente, se trata como nuevo registro (INSERT)
        if (cuentaBancariaTitular.getCodigo() != null && cuentaBancariaTitular.getCodigo() == 0L) {
            cuentaBancariaTitular.setCodigo(null);
        }
        return cuentaBancariaTitularDaoService.save(cuentaBancariaTitular, cuentaBancariaTitular.getCodigo());
    }
}
