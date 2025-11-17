package com.saa.ejb.credito.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.EntidadDaoService;
import com.saa.model.credito.Entidad;

import jakarta.ejb.Stateless;

@Stateless
public class EntidadDaoServiceImpl extends EntityDaoImpl<Entidad> implements EntidadDaoService{

}
