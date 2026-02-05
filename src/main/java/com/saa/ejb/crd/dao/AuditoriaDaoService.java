package com.saa.ejb.crd.dao;

import jakarta.ejb.Local;
import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Auditoria;

@Local
public interface AuditoriaDaoService extends EntityDao<Auditoria> {
	

}
