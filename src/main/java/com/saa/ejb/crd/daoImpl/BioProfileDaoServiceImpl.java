package com.saa.ejb.crd.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.BioProfileDaoService;
import com.saa.model.crd.BioProfile;

import jakarta.ejb.Stateless;

@Stateless
public class BioProfileDaoServiceImpl extends EntityDaoImpl<BioProfile> implements BioProfileDaoService {

}
