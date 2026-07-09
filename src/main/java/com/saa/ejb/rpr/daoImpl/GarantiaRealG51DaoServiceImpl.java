package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.GarantiaRealG51DaoService;
import com.saa.model.rpr.GarantiaRealG51;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class GarantiaRealG51DaoServiceImpl extends EntityDaoImpl<GarantiaRealG51> implements GarantiaRealG51DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "numeroOperacion",
            "numeroGarantia", "tipoGarantia", "descripcionGarantia", "valorAvaluo",
            "fechaAvaluo", "numeroRegistroGarantia", "fechaContabilizacion",
            "porcentajeCubre", "estadoRegistro"
        };
    }
}
