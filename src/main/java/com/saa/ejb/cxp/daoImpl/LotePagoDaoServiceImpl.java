package com.saa.ejb.cxp.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.LotePagoDaoService;
import com.saa.model.cxp.LotePago;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class LotePagoDaoServiceImpl extends EntityDaoImpl<LotePago>
        implements LotePagoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id",
            "empresa",
            "cuentaBancaria",
            "fechaGeneracion",
            "nombreArchivo",
            "path",
            "valorTotal",
            "numeroPagos",
            "estado",
            "observacion",
            "usuario",
            "fechaRegistro"
        };
    }

    @Override
    public List<LotePago> selectByEmpresa(Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectByEmpresa con empresa: " + idEmpresa);
        Query query = em.createQuery(
                " select l from LotePago l " +
                " where  l.empresa.codigo = :idEmpresa " +
                " order by l.id desc");
        query.setParameter("idEmpresa", idEmpresa);
        return query.getResultList();
    }
}
