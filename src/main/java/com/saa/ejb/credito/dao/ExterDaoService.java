package com.saa.ejb.credito.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Exter;

import jakarta.ejb.Remote;

@Remote
public interface ExterDaoService extends EntityDao<Exter>  {

}
