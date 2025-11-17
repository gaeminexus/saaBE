package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoContrato;

import jakarta.ejb.Remote;

@Remote
public interface TipoContratoDaoService extends EntityDao<TipoContrato> {

}
