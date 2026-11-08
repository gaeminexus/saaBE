package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ReglaAprobacionPrestamo;

import jakarta.ejb.Local;

@Local
public interface ReglaAprobacionPrestamoDaoService extends EntityDao<ReglaAprobacionPrestamo> {
    /** Reglas ACTIVAS ({@code RGAPESTD = 1}). Sin try/catch: un error de base no puede pasar por «no hay reglas». */
    List<ReglaAprobacionPrestamo> selectActivas() throws Throwable;

}
