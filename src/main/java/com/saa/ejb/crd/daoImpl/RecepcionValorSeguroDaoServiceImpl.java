package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.RecepcionValorSeguroDaoService;
import com.saa.model.crd.RecepcionValorSeguro;
import com.saa.rubros.CrdEstadoRecepcionSeguro;

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
    public List<RecepcionValorSeguro> selectByReferencia(String referenciaTrim) throws Throwable {
        System.out.println("Ingresa al metodo selectByReferencia de RecepcionValorSeguro"
                + " - referencia: " + referenciaTrim);
        // Igual que el índice: TRIM(RVSGRFRN) y fuera las RECHAZADAS/ANULADAS; NVL(estado,0) del
        // índice equivale a tratar el estado nulo como vigente.
        Query query = em.createQuery(
                " select r from RecepcionValorSeguro r " +
                " where  TRIM(r.referencia) = :referenciaTrim " +
                " and    (r.estado is null or r.estado not in (:rechazado, :anulado)) " +
                " order by r.codigo");
        query.setParameter("referenciaTrim", referenciaTrim);
        query.setParameter("rechazado", Long.valueOf(CrdEstadoRecepcionSeguro.RECHAZADO));
        query.setParameter("anulado", Long.valueOf(CrdEstadoRecepcionSeguro.ANULADO));
        return query.getResultList();
    }

    @Override
    public RecepcionValorSeguro selectParaActualizar(Long idRecepcion) throws Throwable {
        System.out.println("Ingresa al metodo selectParaActualizar de RecepcionValorSeguro"
                + " - recepcion: " + idRecepcion);
        return em.find(RecepcionValorSeguro.class, idRecepcion, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public List<RecepcionValorSeguro> selectSinReferenciaCoincidentes(Long idEntidad, Long idTipoAporte,
            Long idCuentaBancaria, java.time.LocalDate fecha, Double valor) throws Throwable {
        System.out.println("Ingresa al metodo selectSinReferenciaCoincidentes de RecepcionValorSeguro"
                + " - entidad: " + idEntidad + " - tipo: " + idTipoAporte + " - fecha: " + fecha
                + " - valor: " + valor);
        Query query = em.createQuery(
                " select r from RecepcionValorSeguro r " +
                " where  r.entidad.codigo = :idEntidad " +
                " and    r.tipoAporte.codigo = :idTipo " +
                " and    r.cuentaBancaria.codigo = :idCuenta " +
                " and    r.fecha = :fecha " +
                " and    r.valor between :valorMin and :valorMax " +
                " and    TRIM(r.referencia) in ('9', '09') " +
                " and    (r.estado is null or r.estado not in (:rechazado, :anulado)) " +
                " order by r.codigo");
        query.setParameter("idEntidad", idEntidad);
        query.setParameter("idTipo", idTipoAporte);
        query.setParameter("idCuenta", idCuentaBancaria);
        query.setParameter("fecha", fecha);
        query.setParameter("valorMin", valor - 0.005);
        query.setParameter("valorMax", valor + 0.005);
        query.setParameter("rechazado", Long.valueOf(CrdEstadoRecepcionSeguro.RECHAZADO));
        query.setParameter("anulado", Long.valueOf(CrdEstadoRecepcionSeguro.ANULADO));
        return query.getResultList();
    }
}
