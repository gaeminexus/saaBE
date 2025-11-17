package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Filial;

import jakarta.ejb.Remote;

@Remote
public interface FilialDaoService extends EntityDao<Filial> {

}
