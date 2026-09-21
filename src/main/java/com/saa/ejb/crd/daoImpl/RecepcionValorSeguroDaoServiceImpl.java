package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.RecepcionValorSeguroDaoService;
import com.saa.model.crd.RecepcionValorSeguro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class RecepcionValorSeguroDaoServiceImpl extends EntityDaoImpl<RecepcionValorSeguro>
        implements RecepcionValorSeguroDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) RecepcionValorSeguro");
        return new String[]{
            "codigo",
            "entidad",
            "tipoAporte",
            "estado",
            "cuentaBancaria",
            "referencia",
            "rutaRespaldo",
            "valor",
            "fecha",
            "observacion",
            "asiento",
            "aporte",
            "usuarioRegistro",
            "fechaRegistro",
            "usuarioAprobacion",
            "fechaAprobacion",
            "usuarioRechazo",
            "fechaRechazo",
            "motivoRechazo",
            "usuarioAnulacion",
            "fechaAnulacion",
            "motivoAnulacion"
        };
    }

    @Override
    public List<RecepcionValorSeguro> selectByEstado(Long estado) throws Throwable {
        System.out.println("Ingresa al metodo selectByEstado de RecepcionValorSeguro"
                + " - estado: " + estado);
        Query query = em.createQuery(
                " select r from RecepcionValorSeguro r " +
                " where  r.estado = :estado " +
                " order by r.fechaRegistro asc");
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    @Override
    public List<RecepcionValorSeguro> selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad de RecepcionValorSeguro"
                + " - entidad: " + idEntidad);
        Query query = em.createQuery(
                " select r from RecepcionValorSeguro r " +
                " where  r.entidad.codigo = :idEntidad " +
                " order by r.fechaRegistro desc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }

    @Override
    public RecepcionValorSeguro selectParaActualizar(Long idRecepcion) throws Throwable {
        System.out.println("Ingresa al metodo selectParaActualizar de RecepcionValorSeguro"
                + " - recepcion: " + idRecepcion);
        return em.find(RecepcionValorSeguro.class, idRecepcion, LockModeType.PESSIMISTIC_WRITE);
    }
}
