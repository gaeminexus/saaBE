package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Entidad;

import jakarta.ejb.Remote;

@Remote
public interface EntidadDaoService extends EntityDao<Entidad> {

}
