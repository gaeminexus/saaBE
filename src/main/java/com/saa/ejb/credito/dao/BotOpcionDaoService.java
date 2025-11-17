package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.BotOpcion;
import jakarta.ejb.Remote;

@Remote
public interface BotOpcionDaoService extends EntityDao<BotOpcion> {

}
