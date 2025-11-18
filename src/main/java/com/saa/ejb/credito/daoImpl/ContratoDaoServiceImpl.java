package com.saa.ejb.credito.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.ContratoDaoService;
import com.saa.model.credito.Contrato;

import jakarta.ejb.Stateless;

@Stateless
public class ContratoDaoServiceImpl extends EntityDaoImpl<Contrato> implements ContratoDaoService {

}
