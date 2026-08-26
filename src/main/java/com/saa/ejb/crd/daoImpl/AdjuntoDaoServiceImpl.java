package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.AdjuntoDaoService;
import com.saa.model.crd.Adjunto;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class AdjuntoDaoServiceImpl extends EntityDaoImpl<Adjunto> implements AdjuntoDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<Adjunto> selectByReferenciaYTipo(Long idReferencia, Long idTipoAdjunto) {
        System.out.println("AdjuntoDaoServiceImpl - selectByReferenciaYTipo - idReferencia: "
            + idReferencia + " - idTipoAdjunto: " + idTipoAdjunto);
        Query query = em.createQuery(
            " select a from Adjunto a " +
            " where  a.idReferencia = :idReferencia " +
            " and    a.tipoAdjunto.codigo = :idTipoAdjunto " +
            " and    a.estado = :activo " +
            " order by a.fechaRegistro desc");
        query.setParameter("idReferencia", idReferencia);
        query.setParameter("idTipoAdjunto", idTipoAdjunto);
        query.setParameter("activo", (long) Estado.ACTIVO);
        return query.getResultList();
    }
}
