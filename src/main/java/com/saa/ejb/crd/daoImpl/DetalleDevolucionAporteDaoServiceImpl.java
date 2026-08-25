package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DetalleDevolucionAporteDaoService;
import com.saa.model.crd.DetalleDevolucionAporte;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DetalleDevolucionAporteDaoServiceImpl extends EntityDaoImpl<DetalleDevolucionAporte>
        implements DetalleDevolucionAporteDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "devolucion",
            "tipoAporte",
            "valor",
            "idAporte",
            "idPagoAporte",
            "idAporteReverso"
        };
    }

    @Override
    public List<DetalleDevolucionAporte> selectByDevolucion(Long idDevolucion) throws Throwable {
        System.out.println("Ingresa al metodo selectByDevolucion con devolucion: " + idDevolucion);
        Query query = em.createQuery(
                " select d from DetalleDevolucionAporte d " +
                " where  d.devolucion.codigo = :idDevolucion " +
                " order by d.codigo");
        query.setParameter("idDevolucion", idDevolucion);
        return query.getResultList();
    }

    @Override
    public List<DetalleDevolucionAporte> selectByDevoluciones(List<Long> idsDevolucion)
            throws Throwable {
        System.out.println("Ingresa al metodo selectByDevoluciones con "
                + (idsDevolucion != null ? idsDevolucion.size() : 0) + " devoluciones");
        if (idsDevolucion == null || idsDevolucion.isEmpty()) {
            return new ArrayList<>();
        }
        Query query = em.createQuery(
                " select d from DetalleDevolucionAporte d " +
                " where  d.devolucion.codigo in (:idsDevolucion) " +
                " order by d.devolucion.codigo, d.codigo");
        query.setParameter("idsDevolucion", idsDevolucion);
        return query.getResultList();
    }
}
