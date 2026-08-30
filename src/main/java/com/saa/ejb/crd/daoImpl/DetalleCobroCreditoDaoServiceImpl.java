package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DetalleCobroCreditoDaoService;
import com.saa.model.crd.DetalleCobroCredito;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DetalleCobroCreditoDaoServiceImpl extends EntityDaoImpl<DetalleCobroCredito>
        implements DetalleCobroCreditoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) DetalleCobroCredito");
        return new String[]{
            "codigo",
            "cobroCredito",
            "prestamo",
            "valor",
            "modalidad",
            "tipoAporte",
            "periodoDevengo",
            "eventoPrestamo",
            "pagoAporte",
            "observacion"
        };
    }

    @Override
    public List<DetalleCobroCredito> selectByCobro(Long idCobro) throws Throwable {
        System.out.println("Ingresa al metodo selectByCobro de DetalleCobroCredito"
                + " - cobro: " + idCobro);
        Query query = em.createQuery(
                " select d from DetalleCobroCredito d " +
                " where  d.cobroCredito.codigo = :idCobro " +
                " order by d.codigo");
        query.setParameter("idCobro", idCobro);
        return query.getResultList();
    }

    @Override
    public List<DetalleCobroCredito> selectByPrestamo(Long idPrestamo) throws Throwable {
        System.out.println("Ingresa al metodo selectByPrestamo de DetalleCobroCredito"
                + " - prestamo: " + idPrestamo);
        Query query = em.createQuery(
                " select d from DetalleCobroCredito d " +
                " where  d.prestamo.codigo = :idPrestamo " +
                " order by d.codigo desc");
        query.setParameter("idPrestamo", idPrestamo);
        return query.getResultList();
    }
}
