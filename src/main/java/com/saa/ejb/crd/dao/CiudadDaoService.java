package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Ciudad;

import jakarta.ejb.Local;

@Local
public interface CiudadDaoService extends EntityDao<Ciudad> {

}
