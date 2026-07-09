package com.saa.ejb.rpr.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG41DaoService;
import com.saa.model.rpr.HistoricoG41;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG41DaoServiceImpl extends EntityDaoImpl<HistoricoG41> implements HistoricoG41DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "identificacion","tipoIdentificacion","genero","estadoCivil",
            "fechaNacimiento","fechaIngreso","estadoParticipe","tipoSistema","baseCalculoAportacion",
            "tipoRelacionLaboral","estadoRegistro","fechaActualizacionEstado" };
    }
}