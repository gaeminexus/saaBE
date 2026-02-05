package com.saa.ejb.credito.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.PrestamoDaoService;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Stateless;

@Stateless
public class PrestamoDaoServiceImpl extends EntityDaoImpl<Prestamo> implements PrestamoDaoService {

}
