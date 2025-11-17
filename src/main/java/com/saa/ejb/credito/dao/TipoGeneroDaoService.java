package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoGenero;

import jakarta.ejb.Remote;


@Remote
public interface TipoGeneroDaoService extends EntityDao<TipoGenero> {

}
