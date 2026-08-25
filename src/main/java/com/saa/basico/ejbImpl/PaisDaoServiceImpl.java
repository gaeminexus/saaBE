package com.saa.basico.ejbImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.basico.ejb.PaisDaoService;
import com.saa.model.scp.Pais;

import jakarta.ejb.Stateless;

@Stateless
public class PaisDaoServiceImpl extends EntityDaoImpl<Pais> implements PaisDaoService  {

}
