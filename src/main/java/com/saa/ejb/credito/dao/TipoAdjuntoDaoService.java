package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoAdjunto;

import jakarta.ejb.Remote;

@Remote
public interface TipoAdjuntoDaoService  extends EntityDao<TipoAdjunto>  {

}
