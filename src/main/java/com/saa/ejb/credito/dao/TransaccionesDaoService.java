package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Transacciones;

import jakarta.ejb.Local;

@Local
public interface TransaccionesDaoService extends EntityDao<Transacciones> {

}
