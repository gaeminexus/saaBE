/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.GrupoConciliacionExtractoDaoService;
import com.saa.ejb.tsr.service.GrupoConciliacionExtractoService;
import com.saa.model.tsr.GrupoConciliacionExtracto;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de GrupoConciliacionExtractoService.</p>
 */
@Stateless
public class GrupoConciliacionExtractoServiceImpl implements GrupoConciliacionExtractoService {

    @EJB
    private GrupoConciliacionExtractoDaoService grupoConciliacionExtractoDaoService;

    @Override
    public List<GrupoConciliacionExtracto> selectByGrupo(Long idGrupo) throws Throwable {
        return grupoConciliacionExtractoDaoService.selectByGrupo(idGrupo);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        GrupoConciliacionExtracto entidad = new GrupoConciliacionExtracto();
        for (Long registro : id) {
            grupoConciliacionExtractoDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<GrupoConciliacionExtracto> lista) throws Throwable {
        for (GrupoConciliacionExtracto registro : lista) {
            grupoConciliacionExtractoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public GrupoConciliacionExtracto saveSingle(GrupoConciliacionExtracto entidad) throws Throwable {
        return grupoConciliacionExtractoDaoService.save(entidad, entidad.getCodigo());
    }

    @Override
    public List<GrupoConciliacionExtracto> selectAll() throws Throwable {
        return grupoConciliacionExtractoDaoService.selectAll(NombreEntidadesTesoreria.GRUPO_CONCILIACION_EXTRACTO);
    }

    @Override
    public GrupoConciliacionExtracto selectById(Long id) throws Throwable {
        return grupoConciliacionExtractoDaoService.selectById(id, NombreEntidadesTesoreria.GRUPO_CONCILIACION_EXTRACTO);
    }

    @Override
    public List<GrupoConciliacionExtracto> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        return grupoConciliacionExtractoDaoService.selectByCriteria(datos,
                NombreEntidadesTesoreria.GRUPO_CONCILIACION_EXTRACTO);
    }
}
