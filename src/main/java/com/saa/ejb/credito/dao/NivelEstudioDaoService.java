package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.NivelEstudio;

import jakarta.ejb.Remote;

@Remote
public interface NivelEstudioDaoService extends EntityDao<NivelEstudio>{

}
