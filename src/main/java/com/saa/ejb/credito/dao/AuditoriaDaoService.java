package com.saa.ejb.credito.dao;

import jakarta.ejb.Local;
import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Auditoria;

@Local
public interface AuditoriaDaoService extends EntityDao<Auditoria> {
	

}
