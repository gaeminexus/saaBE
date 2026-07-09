package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.NuevoPrestamoG46;

import jakarta.ejb.Local;

@Local
public interface NuevoPrestamoG46DaoService extends EntityDao<NuevoPrestamoG46> {

    /** Retorna todos los registros G46 de un EJRD específico. */
    List<NuevoPrestamoG46> selectByDetalle(Long codigoDetalle) throws Throwable;
}
