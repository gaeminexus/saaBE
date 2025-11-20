package com.saa.ejb.credito.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.credito.EstadoPrestamo;

import jakarta.ejb.Local;

@Local
public interface EstadoPrestamoDaoService extends EntityDao<EstadoPrestamo>  {

}
