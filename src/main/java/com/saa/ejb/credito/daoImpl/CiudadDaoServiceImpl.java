package com.saa.ejb.credito.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.CiudadDaoService;
import com.saa.model.credito.Ciudad;

import jakarta.ejb.Stateless;

@Stateless
public class CiudadDaoServiceImpl extends EntityDaoImpl<Ciudad> implements CiudadDaoService{

}
