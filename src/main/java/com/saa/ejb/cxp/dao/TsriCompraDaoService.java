package com.saa.ejb.cxp.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.Tsri;

import jakarta.ejb.Local;

/**
 * DaoService para la entidad Tsri del módulo CXP.
 */
@Local
public interface TsriCompraDaoService extends EntityDao<Tsri> {
}
