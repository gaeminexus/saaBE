package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.AporteAsoprepDaoService;
import com.saa.ejb.credito.service.AporteAsoprepService;
import com.saa.model.credito.AporteAsoprep;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class AporteAsoprepServiceImpl implements AporteAsoprepService {

    @EJB
    private AporteAsoprepDaoService aporteAsoprepDaoService;

    /**
     * Recupera un registro de AporteAsoprep por su ID.
     */
    @Override
    public AporteAsoprep selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById AporteAsoprep con id: " + id);
        return aporteAsoprepDaoService.selectById(id, NombreEntidadesCredito.APORTE_ASOPREP);
    }

    /**
     * Elimina uno o varios registros de AporteAsoprep.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de AporteAsoprepService ...");
        AporteAsoprep aporteAsoprep = new AporteAsoprep();
        for (Long registro : id) {
            aporteAsoprepDaoService.remove(aporteAsoprep, registro);
        }
    }

    /**
     * Guarda una lista de registros de AporteAsoprep.
     */
    @Override
    public void save(List<AporteAsoprep> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de AporteAsoprepService");
        for (AporteAsoprep registro : lista) {
            aporteAsoprepDaoService.save(registro, registro.getCuenta());
        }
    }

    /**
     * Recupera todos los registros de AporteAsoprep.
     */
    @Override
    public List<AporteAsoprep> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll AporteAsoprepService");
        List<AporteAsoprep> result =
                aporteAsoprepDaoService.selectAll(NombreEntidadesCredito.APORTE_ASOPREP);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total AporteAsoprep no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de AporteAsoprep.
     */
    @Override
    public AporteAsoprep saveSingle(AporteAsoprep aporteAsoprep) throws Throwable {
        System.out.println("saveSingle - AporteAsoprep");
        if(aporteAsoprep.getCuenta() == null)	
        aporteAsoprep = aporteAsoprepDaoService.save(aporteAsoprep, aporteAsoprep.getCuenta());  
        return aporteAsoprep;
    }

    /**
     * Recupera registros de AporteAsoprep según criterios de búsqueda.
     */
    @Override
    public List<AporteAsoprep> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria AporteAsoprepService");

        List<AporteAsoprep> result =
                aporteAsoprepDaoService.selectByCriteria(datos, NombreEntidadesCredito.APORTE_ASOPREP);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio AporteAsoprep no devolvio ningun registro");
        }
        return result;
    }
}
