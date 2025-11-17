package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Producto;

import jakarta.ejb.Remote;

@Remote
public interface ProductoDaoService extends EntityDao<Producto> {

}
