package com.saa.ejb.cxc.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.Factura;

import jakarta.ejb.Local;

@Local
public interface FacturaDaoService extends EntityDao<Factura> {

}
