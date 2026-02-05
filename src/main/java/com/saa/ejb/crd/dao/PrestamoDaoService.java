package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

@Local
public interface PrestamoDaoService extends EntityDao<Prestamo> {

}
