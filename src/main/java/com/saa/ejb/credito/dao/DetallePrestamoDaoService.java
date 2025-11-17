package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.DetallePrestamo;

import jakarta.ejb.Remote;

@Remote
public interface DetallePrestamoDaoService extends EntityDao<DetallePrestamo> {

}
