package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.AcuerdoCondonacionDaoService;
import com.saa.model.crd.AcuerdoCondonacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class AcuerdoCondonacionDaoServiceImpl extends EntityDaoImpl<AcuerdoCondonacion>
        implements AcuerdoCondonacionDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) AcuerdoCondonacion");
        return new String[]{
            "codigo",
            "entidad",
            "prestamo",
            "estado",
            "valorPagar",
            "valorCondonar",
            "valorPagarAportes",
            "valorPagarDeposito",
            "fecha",
            "observacion",
            "usuarioRegistro",
            "fechaRegistro",
            "usuarioAprobacion",
            "fechaAprobacion",
            "usuarioRechazo",
            "fechaRechazo",
            "motivoRechazo",
            "eventoPrestamo",
            "cobroCredito",
            "empresa"
        };
    }

    @Override
    public List<AcuerdoCondonacion> selectByEstado(Long estado) throws Throwable {
        System.out.println("Ingresa al metodo selectByEstado de AcuerdoCondonacion - estado: " + estado);
        Query query = em.createQuery(
                " select a from AcuerdoCondonacion a " +
                " where  a.estado = :estado " +
                " order by a.fechaRegistro asc");
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    @Override
    public List<AcuerdoCondonacion> selectByPrestamo(Long idPrestamo) throws Throwable {
        System.out.println("Ingresa al metodo selectByPrestamo de AcuerdoCondonacion - prestamo: " + idPrestamo);
        Query query = em.createQuery(
                " select a from AcuerdoCondonacion a " +
                " where  a.prestamo.codigo = :idPrestamo " +
                " order by a.fechaRegistro desc");
        query.setParameter("idPrestamo", idPrestamo);
        return query.getResultList();
    }

    @Override
    public List<AcuerdoCondonacion> selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad de AcuerdoCondonacion - entidad: " + idEntidad);
        Query query = em.createQuery(
                " select a from AcuerdoCondonacion a " +
                " where  a.entidad.codigo = :idEntidad " +
                " order by a.fechaRegistro desc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }

    @Override
    public AcuerdoCondonacion selectByCobroCredito(Long idCobro) throws Throwable {
        System.out.println("Ingresa al metodo selectByCobroCredito de AcuerdoCondonacion - cobro: " + idCobro);
        try {
            Query query = em.createQuery(
                    " select a from AcuerdoCondonacion a " +
                    " where  a.cobroCredito.codigo = :idCobro");
            query.setParameter("idCobro", idCobro);
            return (AcuerdoCondonacion) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
