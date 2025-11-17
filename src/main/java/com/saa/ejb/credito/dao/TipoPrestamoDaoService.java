package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoPrestamo;

import jakarta.ejb.Remote;

@Remote
public interface TipoPrestamoDaoService extends EntityDao<TipoPrestamo>  {

}
