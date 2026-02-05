package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Producto;

import jakarta.ejb.Local;

@Local
public interface ProductoDaoService extends EntityDao<Producto> {

}
