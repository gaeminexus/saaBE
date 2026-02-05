package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Comentario;

import jakarta.ejb.Local;

@Local
public interface ComentarioDaoService extends EntityDao<Comentario> {

}
