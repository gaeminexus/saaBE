package com.saa.ejb.cxp.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cxp.Tsri;

import jakarta.ejb.Local;

/**
 * Servicio para la entidad Tsri del módulo CXP.
 */
@Local
public interface TsriCompraService extends EntityService<Tsri> {

    Tsri selectById(Long id) throws Throwable;

    Tsri saveSingle(Tsri tsri) throws Throwable;
}
