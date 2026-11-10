package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.FirmanteContratoDaoService;
import com.saa.model.crd.FirmanteContrato;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class FirmanteContratoDaoServiceImpl extends EntityDaoImpl<FirmanteContrato> implements FirmanteContratoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "idFirma", "orden", "rol", "identificacion", "nombre", "correo", "celular", "idEntidad",
            "estado", "fechaFirma", "idExterno"
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<FirmanteContrato> selectByFirma(Long idFirma) throws Throwable {
        System.out.println("Ingresa al metodo selectByFirma de FirmanteContrato con idFirma: " + idFirma);
        Query query = em.createQuery(
            " select f from FirmanteContrato f where f.idFirma = :idFirma order by f.orden");
        query.setParameter("idFirma", idFirma);
        return query.getResultList();
    }
}
