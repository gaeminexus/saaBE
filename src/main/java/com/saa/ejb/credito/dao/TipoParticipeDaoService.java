package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoParticipe;

import jakarta.ejb.Remote;

@Remote
public interface TipoParticipeDaoService extends EntityDao<TipoParticipe> {

}
