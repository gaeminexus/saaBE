package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.CancelacionG49DaoService;
import com.saa.model.rpr.CancelacionG49;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class CancelacionG49DaoServiceImpl extends EntityDaoImpl<CancelacionG49> implements CancelacionG49DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "numeroOperacion",
            "fechaCancelacion", "formaCancelacion"
        };
    }

    @Override
    public List<CancelacionG49> selectByDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("CancelacionG49DaoServiceImpl.selectByDetalle codigoDetalle: " + codigoDetalle);
        String jpql = "SELECT c FROM CancelacionG49 c WHERE c.detalleEjecucion.codigo = :codigoDetalle";
        return em.createQuery(jpql, CancelacionG49.class)
                .setParameter("codigoDetalle", codigoDetalle)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> selectCancelacionReal(java.time.LocalDate desde, java.time.LocalDate hasta) throws Throwable {
        // D-038: "la fecha en la que se realizó dicha cancelación" (manual de crédito §3.5, campo 4), dentro del
        // mes (§5.5). Prioridad: 1) novación en el mes (fecha del préstamo hijo, CRD.RLPR) → forma O;
        // 2) último evento de pago del préstamo en el mes (CRD.EVPR: precancelación, pago manual, aportes...);
        // 3) último pago vigente del mes (CRD.PGPR); 4) última cuota marcada pagada en el mes (DTPRFCPG).
        // Los pagos de la migración no son confiables, pero los del mes reportado (jun-2025 en adelante) son
        // movimientos del SAA. Sin ningún evento, el generador deja el fin de mes.
        String sql =
            " SELECT x.op, x.fc, x.forma FROM ( "
          + "  SELECT TO_CHAR(p.PRSTIDAS) op, "
          + "   COALESCE( "
          + "    (SELECT MAX(TRUNC(h.PRSTFCHA)) FROM CRD.RLPR rl JOIN CRD.PRST h ON h.PRSTCDGO = rl.PRSTCDGH "
          + "      WHERE rl.PRSTCDGP = p.PRSTCDGO AND rl.RLPRIDST = 1 "
          + "        AND h.PRSTFCHA >= :desde AND h.PRSTFCHA < :hastaSig), "
          + "    (SELECT MAX(TRUNC(e.EVPRFCHA)) FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1 "
          + "        AND e.EVPRFCHA >= :desde AND e.EVPRFCHA < :hastaSig), "
          + "    (SELECT TRUNC(MAX(g.PGPRFCHA)) FROM CRD.PGPR g WHERE g.PRSTCDGO = p.PRSTCDGO AND NVL(g.PGPRANUL, 0) = 0 "
          + "        AND g.PGPRFCHA >= :desde AND g.PGPRFCHA < :hastaSig), "
          + "    (SELECT TRUNC(MAX(d.DTPRFCPG)) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO AND d.DTPRESTD IN (4, 7) "
          + "        AND d.DTPRFCPG >= :desde AND d.DTPRFCPG < :hastaSig)) fc, "
          + "   CASE WHEN EXISTS (SELECT 1 FROM CRD.RLPR rl JOIN CRD.PRST h ON h.PRSTCDGO = rl.PRSTCDGH "
          + "      WHERE rl.PRSTCDGP = p.PRSTCDGO AND rl.RLPRIDST = 1 "
          + "        AND h.PRSTFCHA >= :desde AND h.PRSTFCHA < :hastaSig) "
          + "        THEN 'O' ELSE 'N' END forma "
          + "  FROM CRD.PRST p WHERE p.PRSTIDAS IS NOT NULL) x "
          + " WHERE x.fc IS NOT NULL ";
        return em.createNativeQuery(sql)
                .setParameter("desde", java.sql.Date.valueOf(desde))
                .setParameter("hastaSig", java.sql.Date.valueOf(hasta.plusDays(1)))
                .getResultList();
    }
}
