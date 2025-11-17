package com.saa.ejb.credito.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.credito.EstadoParticipe;

import jakarta.ejb.Remote;

@Remote
public interface EstadoParticipeDaoService extends EntityDao<EstadoParticipe>  {

}
