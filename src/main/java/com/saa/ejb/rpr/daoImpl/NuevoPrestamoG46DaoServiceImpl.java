package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.NuevoPrestamoG46DaoService;
import com.saa.model.rpr.NuevoPrestamoG46;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class NuevoPrestamoG46DaoServiceImpl extends EntityDaoImpl<NuevoPrestamoG46> implements NuevoPrestamoG46DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "numeroOperacion",
            "tipoCredito", "estadoOperacion", "situacionOperacion", "destinoProvincia",
            "destinoCanton", "destinoParroquia", "fechaConcesion", "fechaVencimiento",
            "valorOperacion", "tasaInteresNominal", "periodicidadPago",
            "frecuenciaRevision", "garantias"
        };
    }
}
