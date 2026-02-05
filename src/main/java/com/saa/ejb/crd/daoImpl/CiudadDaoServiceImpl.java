package com.saa.ejb.crd.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CiudadDaoService;
import com.saa.model.crd.Ciudad;

import jakarta.ejb.Stateless;

@Stateless
public class CiudadDaoServiceImpl extends EntityDaoImpl<Ciudad> implements CiudadDaoService{

}
