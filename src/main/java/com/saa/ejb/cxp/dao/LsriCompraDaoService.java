package com.saa.ejb.cxp.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.Lsri;

import jakarta.ejb.Local;

/**
 * DaoService para la entidad Lsri del módulo CXP.
 */
@Local
public interface LsriCompraDaoService extends EntityDao<Lsri> {
}
