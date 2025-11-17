package com.saa.ejb.credito.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.credito.CreditoMontoAprobacion;

import jakarta.ejb.Remote;

@Remote
public interface CreditoMontoAprobacionDaoService extends EntityDao<CreditoMontoAprobacion>  {

}
