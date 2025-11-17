package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoCesantia;
import jakarta.ejb.Remote;


@Remote
public interface TipoCesantiaDaoService extends EntityDao<TipoCesantia> {

}
