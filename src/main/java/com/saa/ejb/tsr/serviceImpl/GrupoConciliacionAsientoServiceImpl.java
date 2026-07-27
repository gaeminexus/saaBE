/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.GrupoConciliacionAsientoDaoService;
import com.saa.ejb.tsr.service.GrupoConciliacionAsientoService;
import com.saa.model.tsr.GrupoConciliacionAsiento;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de GrupoConciliacionAsientoService.</p>
 */
@Stateless
public class GrupoConciliacionAsientoServiceImpl implements GrupoConciliacionAsientoService {

    @EJB
    private GrupoConciliacionAsientoDaoService grupoConciliacionAsientoDaoService;

    @Override
    public List<GrupoConciliacionAsiento> selectByGrupo(Long idGrupo) throws Throwable {
        return grupoConciliacionAsientoDaoService.selectByGrupo(idGrupo);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        GrupoConciliacionAsiento entidad = new GrupoConciliacionAsiento();
        for (Long registro : id) {
            grupoConciliacionAsientoDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<GrupoConciliacionAsiento> lista) throws Throwable {
        for (GrupoConciliacionAsiento registro : lista) {
            grupoConciliacionAsientoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public GrupoConciliacionAsiento saveSingle(GrupoConciliacionAsiento entidad) throws Throwable {
        return grupoConciliacionAsientoDaoService.save(entidad, entidad.getCodigo());
    }

    @Override
    public List<GrupoConciliacionAsiento> selectAll() throws Throwable {
        return grupoConciliacionAsientoDaoService.selectAll(NombreEntidadesTesoreria.GRUPO_CONCILIACION_ASIENTO);
    }

    @Override
    public GrupoConciliacionAsiento selectById(Long id) throws Throwable {
        return grupoConciliacionAsientoDaoService.selectById(id, NombreEntidadesTesoreria.GRUPO_CONCILIACION_ASIENTO);
    }

    @Override
    public List<GrupoConciliacionAsiento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        return grupoConciliacionAsientoDaoService.selectByCriteria(datos,
                NombreEntidadesTesoreria.GRUPO_CONCILIACION_ASIENTO);
    }
}
