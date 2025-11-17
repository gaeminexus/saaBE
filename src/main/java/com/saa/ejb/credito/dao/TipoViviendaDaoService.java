package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoVivienda;

import jakarta.ejb.Remote;

@Remote
public interface TipoViviendaDaoService extends EntityDao<TipoVivienda>  {

}
