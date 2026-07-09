package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.ParticipeCesanteG43DaoService;
import com.saa.model.rpr.ParticipeCesanteG43;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ParticipeCesanteG43DaoServiceImpl extends EntityDaoImpl<ParticipeCesanteG43> implements ParticipeCesanteG43DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "fechaTerminoRelacionLaboral",
            "numeroImposicionesPersonales", "numeroImposicionesPatronales", "fechaLiquidacion",
            "saldoCuentaIndividual", "valoresCompensados", "valoresPagados"
        };
    }
}
