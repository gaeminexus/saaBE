package com.saa.basico.ejb;

import com.saa.basico.util.EntityDao;
import com.saa.model.scp.Pais;

import jakarta.ejb.Local;

@Local
public interface PaisDaoService extends EntityDao<Pais>  {

}
