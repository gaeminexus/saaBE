package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.NivelAprobacionPrestamo;

import jakarta.ejb.Local;

@Local
public interface NivelAprobacionPrestamoDaoService extends EntityDao<NivelAprobacionPrestamo> {
    /** Niveles ACTIVOS de una regla, por orden ascendente. */
    List<NivelAprobacionPrestamo> selectActivosByRegla(Long idRegla) throws Throwable;

}
