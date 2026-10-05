package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.MovimientoInteresCuotaDaoService;
import com.saa.model.crd.MovimientoInteresCuota;
import com.saa.rubros.ComponenteMovimientoInteresCuota;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class MovimientoInteresCuotaDaoServiceImpl extends EntityDaoImpl<MovimientoInteresCuota>
        implements MovimientoInteresCuotaDaoService {

    @PersistenceContext
    EntityManager em;

    private static final int TAMANIO_BLOQUE = 900;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) MovimientoInteresCuota");
        return new String[]{
            "codigo", "prestamo", "idCuota", "tipoMovimiento", "componente", "valor",
            "fechaContable", "corrida", "pago", "origen", "idOrigen", "asiento",
            "movimientoReversado", "tipoCartera", "idBanda", "anulado",
            "usuarioRegistro", "fechaRegistro"
        };
    }

    @Override
    public List<Object[]> selectSaldoProvisionadoPorCuotas(List<Long> idsCuota) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectSaldoProvisionadoPorCuotas - cuotas: "
            + (idsCuota != null ? idsCuota.size() : 0));
        if (idsCuota == null || idsCuota.isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "select m.idCuota, m.componente, "
            + " sum(case when m.tipoMovimiento in (1, 4) then m.valor"
            + "          when m.tipoMovimiento in (2, 3, 9) then -m.valor"
            + "          else 0.0 end)"
            + " from MovimientoInteresCuota m"
            + " where m.idCuota in :idsCuota and m.anulado = 0"
            + " and m.componente in (" + ComponenteMovimientoInteresCuota.INTERES + ", "
            + ComponenteMovimientoInteresCuota.MORA + ")"
            + " group by m.idCuota, m.componente";

        List<Object[]> resultado = new ArrayList<>();
        for (int inicio = 0; inicio < idsCuota.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsCuota.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsCuota.size()));
            Query query = em.createQuery(jpql);
            query.setParameter("idsCuota", bloque);
            resultado.addAll(query.getResultList());
        }
        return resultado;
    }

    @Override
    public int anularByCorrida(Long idCorrida, String usuario) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.anularByCorrida - corrida: " + idCorrida
            + " - usuario: " + usuario);
        if (idCorrida == null) {
            return 0;
        }
        Query query = em.createQuery(
            "update MovimientoInteresCuota m set m.anulado = 1"
            + " where m.corrida.codigo = :idCorrida and m.anulado = 0"
            + " and m.tipoMovimiento in (1, 5, 7)");
        query.setParameter("idCorrida", idCorrida);
        return query.executeUpdate();
    }

    @Override
    public List<MovimientoInteresCuota> selectVigentesPorPagosYTipos(List<Long> idsPago, List<Long> tipos)
            throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectVigentesPorPagosYTipos - pagos: "
            + (idsPago != null ? idsPago.size() : 0) + " - tipos: " + tipos);
        if (idsPago == null || idsPago.isEmpty() || tipos == null || tipos.isEmpty()) {
            return new ArrayList<>();
        }
        List<MovimientoInteresCuota> resultado = new ArrayList<>();
        for (int inicio = 0; inicio < idsPago.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsPago.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsPago.size()));
            Query query = em.createQuery(
                "select m from MovimientoInteresCuota m"
                + " where m.pago.codigo in :idsPago and m.tipoMovimiento in :tipos and m.anulado = 0"
                + " and not exists (select 1 from MovimientoInteresCuota r"
                + "     where r.movimientoReversado = m and r.anulado = 0)");
            query.setParameter("idsPago", bloque);
            query.setParameter("tipos", tipos);
            resultado.addAll(query.getResultList());
        }
        return resultado;
    }

    @Override
    public java.util.Map<Long, Double> selectSaldoDevengadoPorCuotas(List<Long> idsCuota) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.selectSaldoDevengadoPorCuotas - cuotas: "
            + (idsCuota != null ? idsCuota.size() : 0));
        java.util.Map<Long, Double> resultado = new java.util.HashMap<>();
        if (idsCuota == null || idsCuota.isEmpty()) {
            return resultado;
        }
        String jpql = "select m.idCuota, "
            + " sum(case when m.tipoMovimiento = 5 then m.valor when m.tipoMovimiento = 6 then -m.valor"
            + "          else 0.0 end)"
            + " from MovimientoInteresCuota m"
            + " where m.idCuota in :idsCuota and m.anulado = 0 and m.tipoMovimiento in (5, 6)"
            + " group by m.idCuota";
        for (int inicio = 0; inicio < idsCuota.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = idsCuota.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, idsCuota.size()));
            Query query = em.createQuery(jpql);
            query.setParameter("idsCuota", bloque);
            for (Object[] fila : (List<Object[]>) query.getResultList()) {
                Long idCuota = (Long) fila[0];
                double saldo = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
                resultado.put(idCuota, saldo);
            }
        }
        return resultado;
    }

    @Override
    public boolean existeAlgunoByCorrida(Long idCorrida) throws Throwable {
        System.out.println("MovimientoInteresCuotaDaoServiceImpl.existeAlgunoByCorrida - corrida: " + idCorrida);
        if (idCorrida == null) {
            return false;
        }
        Query query = em.createQuery(
            "select count(m) from MovimientoInteresCuota m where m.corrida.codigo = :idCorrida");
        query.setParameter("idCorrida", idCorrida);
        Long cantidad = (Long) query.getSingleResult();
        return cantidad != null && cantidad > 0L;
    }
}
