package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Aporte;

import jakarta.ejb.Remote;

@Remote
public interface AporteDaoService extends EntityDao<Aporte>  {

}
