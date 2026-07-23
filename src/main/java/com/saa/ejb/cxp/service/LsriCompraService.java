package com.saa.ejb.cxp.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cxp.Lsri;

import jakarta.ejb.Local;

/**
 * Servicio para la entidad Lsri del módulo CXP.
 */
@Local
public interface LsriCompraService extends EntityService<Lsri> {

    Lsri selectById(Long id) throws Throwable;

    Lsri saveSingle(Lsri lsri) throws Throwable;
}
