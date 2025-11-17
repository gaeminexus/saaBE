package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.TipoIdentificacion;

import jakarta.ejb.Remote;

@Remote
public interface TipoIdentificacionDaoService  extends EntityDao<TipoIdentificacion> {

}
