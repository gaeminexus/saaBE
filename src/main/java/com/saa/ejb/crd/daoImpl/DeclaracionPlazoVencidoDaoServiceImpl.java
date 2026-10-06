package com.saa.ejb.crd.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DeclaracionPlazoVencidoDaoService;
import com.saa.model.crd.DeclaracionPlazoVencido;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DeclaracionPlazoVencidoDaoServiceImpl extends EntityDaoImpl<DeclaracionPlazoVencido>
        implements DeclaracionPlazoVencidoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) DeclaracionPlazoVencido");
        return new String[]{
            "codigo",
            "prestamo",
            "estado",
            "estadoAnterior",
            "numeroMemorando",
            "fechaCorte",
            "paraNombre",
            "paraCargo",
            "ccNombre",
            "ccCargo",
            "numeroPrestamoImpreso",
            "nombreParticipe",
            "cedula",
            "tipoCredito",
            "fechaInicial",
            "fechaFinal",
            "fechaUltimoCobro",
            "fechaInicioMora",
            "monto",
            "capitalCobrado",
            "saldoCapital",
            "interesDevengado",
            "interesCobrado",
            "saldoInteres",
            "desgravamenDevengado",
            "desgravamenCobrado",
            "saldoDesgravamen",
            "seguroDevengado",
            "seguroCobrado",
            "saldoSeguro",
            "moraDevengada",
            "moraCobrada",
            "saldoMora",
            "totalCobrado",
            "totalPorCobrar",
            "dividendoMensual",
            "cuotasPlazo",
            "cuotasCobradas",
            "cuotasPendientes",
            "cuotasPorVencer",
            "usuarioDeclaracion",
            "fechaDeclaracion",
            "fechaCorteLiquidacion",
            "liquidacionSaldoCapital",
            "liquidacionInteres",
            "liquidacionDesgravamen",
            "liquidacionSeguro",
            "liquidacionMora",
            "liquidacionTotal",
            "liquidacionCuotasImpagas",
            "usuarioLiquidacion",
            "fechaLiquidacion",
            "usuarioReverso",
            "fechaReverso",
            "motivoReverso"
        };
    }

    @Override
    public List<DeclaracionPlazoVencido> selectByFiltros(Long estado, LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("DeclaracionPlazoVencidoDaoServiceImpl.selectByFiltros - estado: " + estado
            + " - desde: " + desde + " - hasta: " + hasta);

        StringBuilder jpql = new StringBuilder("select d from DeclaracionPlazoVencido d where 1 = 1");
        if (estado != null) {
            jpql.append(" and d.estado = :estado");
        }
        if (desde != null) {
            jpql.append(" and d.fechaCorte >= :desde");
        }
        if (hasta != null) {
            jpql.append(" and d.fechaCorte <= :hasta");
        }
        jpql.append(" order by d.fechaCorte desc, d.codigo desc");

        Query query = em.createQuery(jpql.toString());
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        if (desde != null) {
            query.setParameter("desde", desde);
        }
        if (hasta != null) {
            query.setParameter("hasta", hasta);
        }
        return query.getResultList();
    }

    @Override
    public DeclaracionPlazoVencido selectVivaByPrestamo(Long idPrestamo) throws Throwable {
        System.out.println("DeclaracionPlazoVencidoDaoServiceImpl.selectVivaByPrestamo - idPrestamo: " + idPrestamo);
        Query query = em.createQuery(
            "select d from DeclaracionPlazoVencido d " +
            "where d.prestamo.codigo = :idPrestamo " +
            "and d.estado in (:declarada, :liquidada) " +
            "order by d.codigo desc");
        query.setParameter("idPrestamo", idPrestamo);
        query.setParameter("declarada", DeclaracionPlazoVencido.ESTADO_DECLARADA);
        query.setParameter("liquidada", DeclaracionPlazoVencido.ESTADO_LIQUIDADA);
        query.setMaxResults(1);
        List<DeclaracionPlazoVencido> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public DeclaracionPlazoVencido selectByNumeroMemorandoNormalizado(String numeroMemorando) throws Throwable {
        System.out.println("DeclaracionPlazoVencidoDaoServiceImpl.selectByNumeroMemorandoNormalizado - numero: "
            + numeroMemorando);
        if (numeroMemorando == null) {
            return null;
        }
        Query query = em.createQuery(
            "select d from DeclaracionPlazoVencido d " +
            "where upper(trim(d.numeroMemorando)) = :numero");
        query.setParameter("numero", numeroMemorando.trim().toUpperCase());
        List<DeclaracionPlazoVencido> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public DeclaracionPlazoVencido selectUltima() throws Throwable {
        System.out.println("DeclaracionPlazoVencidoDaoServiceImpl.selectUltima");
        try {
            Query query = em.createQuery(
                "select d from DeclaracionPlazoVencido d " +
                "order by d.fechaDeclaracion desc, d.codigo desc");
            query.setMaxResults(1);
            return (DeclaracionPlazoVencido) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<DeclaracionPlazoVencido> selectVivasByPrestamosYRango(List<Long> idsPrestamo,
            java.time.LocalDateTime desde, java.time.LocalDateTime hasta) throws Throwable {
        System.out.println("DeclaracionPlazoVencidoDaoServiceImpl.selectVivasByPrestamosYRango - préstamos: "
            + (idsPrestamo != null ? idsPrestamo.size() : 0));
        if (idsPrestamo == null || idsPrestamo.isEmpty() || desde == null || hasta == null) {
            return new java.util.ArrayList<>();
        }
        List<DeclaracionPlazoVencido> resultado = new java.util.ArrayList<>();
        int tamanoBloque = 900;
        for (int inicio = 0; inicio < idsPrestamo.size(); inicio += tamanoBloque) {
            List<Long> bloque = idsPrestamo.subList(inicio, Math.min(inicio + tamanoBloque, idsPrestamo.size()));
            Query query = em.createQuery(
                "select d from DeclaracionPlazoVencido d where d.prestamo.codigo in :idsPrestamo"
                + " and d.estado in (:declarada, :liquidada)"
                + " and d.fechaDeclaracion between :desde and :hasta"
                + " order by d.prestamo.codigo asc, d.fechaDeclaracion asc");
            query.setParameter("idsPrestamo", bloque);
            query.setParameter("declarada", DeclaracionPlazoVencido.ESTADO_DECLARADA);
            query.setParameter("liquidada", DeclaracionPlazoVencido.ESTADO_LIQUIDADA);
            query.setParameter("desde", desde);
            query.setParameter("hasta", hasta);
            resultado.addAll(query.getResultList());
        }
        return resultado;
    }

    @Override
    public java.util.Map<Long, java.time.LocalDate> selectFechaCorteVivaByPrestamos(List<Long> idsPrestamo)
            throws Throwable {
        System.out.println("DeclaracionPlazoVencidoDaoServiceImpl.selectFechaCorteVivaByPrestamos - préstamos: "
            + (idsPrestamo != null ? idsPrestamo.size() : 0));
        java.util.Map<Long, java.time.LocalDate> resultado = new java.util.HashMap<>();
        if (idsPrestamo == null || idsPrestamo.isEmpty()) {
            return resultado;
        }
        int tamanoBloque = 900;
        for (int inicio = 0; inicio < idsPrestamo.size(); inicio += tamanoBloque) {
            List<Long> bloque = idsPrestamo.subList(inicio, Math.min(inicio + tamanoBloque, idsPrestamo.size()));
            Query query = em.createQuery(
                "select d.prestamo.codigo, d.fechaCorte from DeclaracionPlazoVencido d"
                + " where d.prestamo.codigo in :idsPrestamo and d.estado in (:declarada, :liquidada)");
            query.setParameter("idsPrestamo", bloque);
            query.setParameter("declarada", DeclaracionPlazoVencido.ESTADO_DECLARADA);
            query.setParameter("liquidada", DeclaracionPlazoVencido.ESTADO_LIQUIDADA);
            for (Object[] fila : (List<Object[]>) query.getResultList()) {
                resultado.put((Long) fila[0], (java.time.LocalDate) fila[1]);
            }
        }
        return resultado;
    }
}
