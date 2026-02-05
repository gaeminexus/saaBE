package com.saa.ejb.credito.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.ComentarioDaoService;
import com.saa.model.crd.Comentario;

import jakarta.ejb.Stateless;


@Stateless
public class ComentarioDaoServiceImpl extends EntityDaoImpl<Comentario> implements ComentarioDaoService{

}
