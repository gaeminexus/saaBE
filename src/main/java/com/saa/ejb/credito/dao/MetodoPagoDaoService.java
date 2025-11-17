package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.MetodoPago;

import jakarta.ejb.Remote;

@Remote
public interface MetodoPagoDaoService  extends EntityDao<MetodoPago>{

}
