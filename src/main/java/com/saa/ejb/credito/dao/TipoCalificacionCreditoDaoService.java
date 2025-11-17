package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoCalificacionCredito;

import jakarta.ejb.Remote;

@Remote
public interface TipoCalificacionCreditoDaoService extends EntityDao<TipoCalificacionCredito> {

}
