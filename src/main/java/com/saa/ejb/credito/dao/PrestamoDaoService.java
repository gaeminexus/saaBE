package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Prestamo;

import jakarta.ejb.Remote;

@Remote
public interface PrestamoDaoService extends EntityDao<Prestamo> {

}
