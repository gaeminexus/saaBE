package com.saa.ejb.cxp.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.AnticipoProveedorDaoService;
import com.saa.model.cxp.AnticipoProveedor;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class AnticipoProveedorDaoServiceImpl extends EntityDaoImpl<AnticipoProveedor>
        implements AnticipoProveedorDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id",
            "titular",
            "empresa",
            "fechaAnticipo",
            "fechaRecepcion",
            "numeroDoc",
            "valor",
            "saldo",
            "formaPago",
            "referencia",
            "banco",
            "observacion",
            "estado",
            "usuario",
            "asiento",
            "fechaRegistro"
        };
    }
}
