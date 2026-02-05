package com.saa.ejb.credito.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.BioProfile;

import jakarta.ejb.Local;

@Local
public interface BioProfileDaoService extends EntityDao<BioProfile> {

}
