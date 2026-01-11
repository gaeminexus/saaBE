package com.saa.ejb.credito.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.TransaccionesDaoService;
import com.saa.model.credito.Transacciones;

import jakarta.ejb.Stateless;

@Stateless
public class TransaccionesDaoServiceImpl extends EntityDaoImpl<Transacciones> implements TransaccionesDaoService{

}
