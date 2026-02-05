package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

@Local
public interface PrestamoDaoService extends EntityDao<Prestamo> {

}
