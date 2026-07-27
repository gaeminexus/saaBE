/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.ejb.tsr.dao.GrupoConciliacionContableDaoService;
import com.saa.ejb.tsr.service.GrupoConciliacionContableService;
import com.saa.model.tsr.GrupoConciliacionContable;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.basico.util.DatosBusqueda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de GrupoConciliacionContableService.</p>
 */
@Stateless
public class GrupoConciliacionContableServiceImpl implements GrupoConciliacionContableService {

    @EJB
    private GrupoConciliacionContableDaoService grupoConciliacionContableDaoService;

    @Override
    public List<GrupoConciliacionContable> selectActivosByConciliacion(Long idConciliacionContable) throws Throwable {
        return grupoConciliacionContableDaoService.selectActivosByConciliacion(idConciliacionContable);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        GrupoConciliacionContable grupo = new GrupoConciliacionContable();
        for (Long registro : id) {
            grupoConciliacionContableDaoService.remove(grupo, registro);
        }
    }

    @Override
    public void save(List<GrupoConciliacionContable> lista) throws Throwable {
        for (GrupoConciliacionContable registro : lista) {
            grupoConciliacionContableDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public GrupoConciliacionContable saveSingle(GrupoConciliacionContable grupo) throws Throwable {
        return grupoConciliacionContableDaoService.save(grupo, grupo.getCodigo());
    }

    @Override
    public List<GrupoConciliacionContable> selectAll() throws Throwable {
        return grupoConciliacionContableDaoService.selectAll(NombreEntidadesTesoreria.GRUPO_CONCILIACION_CONTABLE);
    }

    @Override
    public GrupoConciliacionContable selectById(Long id) throws Throwable {
        return grupoConciliacionContableDaoService.selectById(id, NombreEntidadesTesoreria.GRUPO_CONCILIACION_CONTABLE);
    }

    @Override
    public List<GrupoConciliacionContable> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        return grupoConciliacionContableDaoService.selectByCriteria(datos,
                NombreEntidadesTesoreria.GRUPO_CONCILIACION_CONTABLE);
    }
}
