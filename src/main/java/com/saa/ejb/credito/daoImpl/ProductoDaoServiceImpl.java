package com.saa.ejb.credito.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.ProductoDaoService;
import com.saa.model.credito.Producto;

import jakarta.ejb.Stateless;

@Stateless
public class ProductoDaoServiceImpl extends EntityDaoImpl<Producto> implements ProductoDaoService {

}
