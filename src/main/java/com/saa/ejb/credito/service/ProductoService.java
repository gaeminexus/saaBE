package com.saa.ejb.credito.service;

import com.saa.basico.util.EntityService;
import com.saa.model.credito.Producto;

import jakarta.ejb.Remote;

@Remote
public interface ProductoService extends EntityService<Producto> {

}
