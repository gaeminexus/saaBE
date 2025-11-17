package com.saa.ejb.credito.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.credito.EstadoCesantia;

import jakarta.ejb.Remote;

@Remote
public interface EstadoCesantiaDaoService extends EntityDao<EstadoCesantia>  {

}
