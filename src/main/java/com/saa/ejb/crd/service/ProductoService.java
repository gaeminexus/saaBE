package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Producto;

import jakarta.ejb.Local;

@Local
public interface ProductoService extends EntityService<Producto> {

}
