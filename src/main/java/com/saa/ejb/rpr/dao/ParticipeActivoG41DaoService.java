package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.ParticipeActivoG41;
import jakarta.ejb.Local;

@Local
public interface ParticipeActivoG41DaoService extends EntityDao<ParticipeActivoG41> {

    /**
     * Retorna todos los registros de CG41 que pertenecen a un EJRD específico.
     * Usado por G45 para obtener las identificaciones de los nuevos partícipes
     * (ya que G41 cambia idEstado 1→10 antes de que corra G45).
     *
     * @param codigoDetalle Código del EJRD
     * @return Lista de ParticipeActivoG41
     */
    List<ParticipeActivoG41> selectByDetalle(Long codigoDetalle) throws Throwable;
}
