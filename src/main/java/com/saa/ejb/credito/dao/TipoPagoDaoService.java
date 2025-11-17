package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoPago;

import jakarta.ejb.Remote;

@Remote
public interface TipoPagoDaoService extends EntityDao<TipoPago> {

}
