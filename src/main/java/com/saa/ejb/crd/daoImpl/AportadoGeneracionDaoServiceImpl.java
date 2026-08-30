package com.saa.ejb.crd.daoImpl;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.dao.AportadoGeneracionDaoService;
import com.saa.rubros.EstadoParticipeEntidad;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @see AportadoGeneracionDaoService
 *
 * Vive en {@code com.saa.ejb.crd.daoImpl} (no en un package aparte) porque necesita
 * {@link PeriodoEfectivoAporteSql}, package-private, para no redefinir la expresión del
 * periodo efectivo del aporte fuera de su única fuente.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
@Stateless
public class AportadoGeneracionDaoServiceImpl implements AportadoGeneracionDaoService {

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> sumAportadoPorEntidadPeriodoTipo(Long codigoFilial, LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo sumAportadoPorEntidadPeriodoTipo con codigoFilial: " + codigoFilial
            + " - desde: " + desde + " - hasta: " + hasta);

        Query query = em.createNativeQuery(
            " SELECT a.ENTDCDGO, " + PeriodoEfectivoAporteSql.PERIODO_EFECTIVO_SQL + " AS PERIODO, "
            + "        a.TPAPCDGO, SUM(a.APRTVLRR) "
            + " FROM   CRD.APRT a "
            + " JOIN   CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO "
            + " WHERE  e.FLLLCDGO = :codigoFilial "
            + " AND    e.ENTDIDST IN (" + EstadoParticipeEntidad.ACTIVO + ", " + EstadoParticipeEntidad.ACTIVO_EN_MORA + ") "
            + " GROUP BY a.ENTDCDGO, " + PeriodoEfectivoAporteSql.PERIODO_EFECTIVO_SQL + ", a.TPAPCDGO "
            + " HAVING " + PeriodoEfectivoAporteSql.PERIODO_EFECTIVO_SQL + " BETWEEN :desde AND :hasta");
        query.setParameter("codigoFilial", codigoFilial);
        query.setParameter("desde", Date.valueOf(desde));
        query.setParameter("hasta", Date.valueOf(hasta));

        List<Object[]> crudas = query.getResultList();
        List<Object[]> resultado = new ArrayList<>();
        for (Object[] fila : crudas) {
            resultado.add(new Object[]{
                fila[0] != null ? ((Number) fila[0]).longValue() : null,
                aFecha(fila[1]),
                fila[2] != null ? ((Number) fila[2]).longValue() : null,
                fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0
            });
        }
        return resultado;
    }

    private LocalDate aFecha(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof java.sql.Date) {
            return ((java.sql.Date) valor).toLocalDate();
        }
        if (valor instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) valor).toLocalDateTime().toLocalDate();
        }
        return null;
    }
}
