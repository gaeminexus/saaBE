package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DetallePlazoVencidoDaoService;
import com.saa.model.crd.DetallePlazoVencido;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DetallePlazoVencidoDaoServiceImpl extends EntityDaoImpl<DetallePlazoVencido>
        implements DetallePlazoVencidoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) DetallePlazoVencido");
        return new String[]{
            "codigo",
            "declaracion",
            "idCuota",
            "desgravamenOriginal",
            "valorSeguroIncendioOriginal",
            "totalOriginal",
            "totalConSeguroOriginal",
            "fechaRestitucion"
        };
    }

    @Override
    public List<DetallePlazoVencido> selectByDeclaracion(Long idDeclaracion) throws Throwable {
        System.out.println("DetallePlazoVencidoDaoServiceImpl.selectByDeclaracion - idDeclaracion: " + idDeclaracion);
        Query query = em.createQuery(
            "select d from DetallePlazoVencido d " +
            "where d.declaracion.codigo = :idDeclaracion " +
            "order by d.codigo asc");
        query.setParameter("idDeclaracion", idDeclaracion);
        return query.getResultList();
    }
}
