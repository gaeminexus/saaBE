package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.NovedadParticipeCargaDaoService;
import com.saa.ejb.crd.service.NovedadParticipeCargaService;
import com.saa.model.crd.NovedadParticipeCarga;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class NovedadParticipeCargaServiceImpl implements NovedadParticipeCargaService {

    @EJB
    private NovedadParticipeCargaDaoService novedadParticipeCargaDaoService;

    @Override
    public NovedadParticipeCarga saveSingle(NovedadParticipeCarga entity) throws Throwable {
        if (entity.getCodigo() == null || entity.getCodigo() == 0) {
            return novedadParticipeCargaDaoService.save(entity, entity.getCodigo());
        } else {
            return novedadParticipeCargaDaoService.save(entity, entity.getCodigo());
        }
    }

    @Override
    public void save(List<NovedadParticipeCarga> entities) throws Throwable {
        System.out.println("Ingresa al metodo save de NovedadParticipeCargaService");
        for (NovedadParticipeCarga entity : entities) {
            novedadParticipeCargaDaoService.save(entity, entity.getCodigo());
        }
    }

    @Override
    public NovedadParticipeCarga selectById(Long id) throws Throwable {
        return novedadParticipeCargaDaoService.selectById(id, NombreEntidadesCredito.NOVEDAD_PARTICIPE_CARGA);
    }

    @Override
    public List<NovedadParticipeCarga> selectAll() throws Throwable {
        return novedadParticipeCargaDaoService.selectAll(NombreEntidadesCredito.NOVEDAD_PARTICIPE_CARGA);
    }

    @Override
    public List<NovedadParticipeCarga> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        return novedadParticipeCargaDaoService.selectByCriteria(datos, NombreEntidadesCredito.NOVEDAD_PARTICIPE_CARGA);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        for (Long id : ids) {
            NovedadParticipeCarga entity = selectById(id);
            if (entity != null) {
                novedadParticipeCargaDaoService.remove(entity, id);
            }
        }
    }

}
