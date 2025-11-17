package com.saa.ejb.contabilidad.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.ReporteCuentaCC;

import jakarta.ejb.Remote;

@Remote
public interface ReporteCuentaCCDaoService extends EntityDao<ReporteCuentaCC> {
    // Interfaz base sin métodos adicionales por ahora  
} 