package com.saa.ejb.cxp.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.LsriCompraDaoService;
import com.saa.model.cxp.Lsri;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Implementación de LsriCompraDaoService (módulo CXP).
 */
@Stateless
public class LsriCompraDaoServiceImpl extends EntityDaoImpl<Lsri> implements LsriCompraDaoService {

    @PersistenceContext
    EntityManager em;

    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) Lsri CXP");
        return new String[]{"id", "tabla", "detalle", "estado"};
    }
}
