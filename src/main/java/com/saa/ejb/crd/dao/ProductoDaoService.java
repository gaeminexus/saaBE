package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Producto;

import jakarta.ejb.Local;

@Local
public interface ProductoDaoService extends EntityDao<Producto> {

}
