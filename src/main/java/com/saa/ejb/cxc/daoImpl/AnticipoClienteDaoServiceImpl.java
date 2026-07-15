package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.AnticipoClienteDaoService;
import com.saa.model.cxc.AnticipoCliente;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class AnticipoClienteDaoServiceImpl extends EntityDaoImpl<AnticipoCliente>
        implements AnticipoClienteDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id",
            "titular",
            "fechaAnticipo",
            "fechaRecepcion",
            "usuario",
            "fechaRegistro",
            "numeroDoc",
            "valor",
            "asiento",
            "estado",
            "empresa",
            "observacion"
        };
    }
}
