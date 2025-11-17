package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Profesion;

import jakarta.ejb.Remote;

@Remote
public interface ProfesionDaoService extends EntityDao<Profesion> {

}
