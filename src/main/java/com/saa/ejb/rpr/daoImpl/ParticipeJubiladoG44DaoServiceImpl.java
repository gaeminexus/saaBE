package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.ParticipeJubiladoG44DaoService;
import com.saa.model.rpr.ParticipeJubiladoG44;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ParticipeJubiladoG44DaoServiceImpl extends EntityDaoImpl<ParticipeJubiladoG44> implements ParticipeJubiladoG44DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "tipoJubilacion",
            "fechaJubilacion", "imposicionesAcumuladas", "valorPension",
            "valorNetoRecibir", "saldoCuenta", "valoresCompensados", "jubilacionIess"
        };
    }
}
