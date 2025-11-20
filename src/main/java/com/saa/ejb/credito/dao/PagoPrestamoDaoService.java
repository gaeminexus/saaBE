package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.PagoPrestamo;

import jakarta.ejb.Local;


@Local
public interface PagoPrestamoDaoService extends EntityDao<PagoPrestamo> {

}
