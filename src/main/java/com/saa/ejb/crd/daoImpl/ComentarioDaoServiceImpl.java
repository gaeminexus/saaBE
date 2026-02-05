package com.saa.ejb.crd.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ComentarioDaoService;
import com.saa.model.crd.Comentario;

import jakarta.ejb.Stateless;


@Stateless
public class ComentarioDaoServiceImpl extends EntityDaoImpl<Comentario> implements ComentarioDaoService{

}
