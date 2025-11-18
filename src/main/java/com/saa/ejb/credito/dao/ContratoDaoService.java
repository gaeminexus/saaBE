package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Contrato;

import jakarta.ejb.Remote;

@Remote
public interface ContratoDaoService extends EntityDao<Contrato> {

}
