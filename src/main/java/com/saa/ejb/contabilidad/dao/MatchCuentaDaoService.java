package com.saa.ejb.contabilidad.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.MatchCuenta;

import jakarta.ejb.Remote;

@Remote
public interface MatchCuentaDaoService  extends EntityDao<MatchCuenta>  {

	
}
