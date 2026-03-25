package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.NovedadParticipeCarga;

import jakarta.ejb.Local;

@Local
public interface NovedadParticipeCargaDaoService extends EntityDao<NovedadParticipeCarga> {

    /**
     * Busca todas las novedades de un partícipe por carga archivo
     * @param codigoParticipe Código del ParticipeXCargaArchivo
     * @return Lista de novedades encontradas
     * @throws Throwable Si ocurre un error
     */
    List<NovedadParticipeCarga> selectByParticipe(Long codigoParticipe) throws Throwable;

}
