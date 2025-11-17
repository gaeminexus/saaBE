package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Pais;

import jakarta.ejb.Remote;

@Remote
public interface PaisDaoService extends EntityDao<Pais>  {

}
