package com.saa.ejb.cxp.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.TsriCompraDaoService;
import com.saa.model.cxp.Tsri;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Implementación de TsriCompraDaoService (módulo CXP).
 */
@Stateless
public class TsriCompraDaoServiceImpl extends EntityDaoImpl<Tsri> implements TsriCompraDaoService {

    @PersistenceContext
    EntityManager em;

    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) Tsri CXP");
        return new String[]{"id", "lsri", "codigo", "detalle", "porcentaje", "valor", "texto", "estado", "planCuenta"};
    }
}
