package com.saa.basico.ejb;

import com.saa.basico.util.EntityService;
import com.saa.model.scp.Pais;

import jakarta.ejb.Local;

@Local
public interface PaisService extends EntityService<Pais> {

}
