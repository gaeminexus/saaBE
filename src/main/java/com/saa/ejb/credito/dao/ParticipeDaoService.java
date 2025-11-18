package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Participe;

import jakarta.ejb.Remote;

@Remote
public interface ParticipeDaoService extends EntityDao<Participe>{

}
