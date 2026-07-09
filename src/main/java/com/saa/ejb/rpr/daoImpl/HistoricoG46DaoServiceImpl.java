package com.saa.ejb.rpr.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG46DaoService;
import com.saa.model.rpr.HistoricoG46;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG46DaoServiceImpl extends EntityDaoImpl<HistoricoG46> implements HistoricoG46DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "numeroOperacion","tipoIdentificacion","identificacion",
            "tipoCredito","estadoOperacion","situacionOperacion","destinoProvincia","destinoCanton",
            "destinoParroquia","fechaConcesion","fechaVencimiento","valorOperacion",
            "tasaInteresNominal","periodicidadPago","frecuenciaRevision","garantesGarantias" };
    }
}