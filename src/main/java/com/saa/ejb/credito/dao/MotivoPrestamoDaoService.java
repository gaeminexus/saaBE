package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.MotivoPrestamo;
import jakarta.ejb.Remote;

@Remote
public interface MotivoPrestamoDaoService extends EntityDao<MotivoPrestamo> {

}
