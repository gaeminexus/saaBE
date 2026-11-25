package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.TarifaIvaService;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @see TarifaIvaService
 * @author Sistema SAA
 * @since 2026-11-25
 */
@Stateless
public class TarifaIvaServiceImpl implements TarifaIvaService {

    private static final String TABLA_IVA_GENERAL = "614";
    private static final int IVA_GENERAL_POR_DEFECTO = 15;

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public String codigoPorPorcentaje(Long porcentaje) throws Exception {
        long p = porcentaje != null ? porcentaje.longValue() : 0L;
        System.out.println("TarifaIvaService.codigoPorPorcentaje - porcentaje: " + p);
        // En CBR.TSRI la columna LSRI guarda el código de tabla del SRI (17 = tarifa del IVA), no el id de CBR.LSRI.
        List<Object> codigos = em.createNativeQuery(
                "SELECT TRIM(t.CODIGO) FROM CBR.TSRI t WHERE TO_CHAR(t.LSRI) = :tabla AND t.ESTADO = 1 AND ROUND(t.PORCENTAJE, 2) = :porc "
                + "ORDER BY TO_NUMBER(REGEXP_SUBSTR(t.CODIGO, '[0-9]+')), t.ID")
            .setParameter("tabla", TABLA_TARIFA_IVA).setParameter("porc", (double) p).getResultList();
        if (codigos.isEmpty() || codigos.get(0) == null) {
            throw new IncomeException("El IVA del " + p + " % no está en el catálogo de tarifas del SRI (tabla " + TABLA_TARIFA_IVA
                + " de Tipos SRI, activas): no se puede resolver su código ni su cuenta contable. Cargue la tarifa antes de contabilizar.");
        }
        return codigos.get(0).toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int porcentajeGeneral(Long porcentajeDocumento) {
        if (porcentajeDocumento != null && porcentajeDocumento.longValue() > 0L) {
            return porcentajeDocumento.intValue();
        }
        try {
            List<Object> filas = em.createNativeQuery(
                    "SELECT t.PORCENTAJE FROM CBR.TSRI t WHERE TO_CHAR(t.LSRI) = :tabla AND t.ESTADO = 1 ORDER BY t.ID DESC")
                .setParameter("tabla", TABLA_IVA_GENERAL).setMaxResults(1).getResultList();
            if (!filas.isEmpty() && filas.get(0) != null) {
                return ((Number) filas.get(0)).intValue();
            }
        } catch (Exception e) {
            System.err.println("⚠ TarifaIvaService.porcentajeGeneral: " + e.getMessage());
        }
        return IVA_GENERAL_POR_DEFECTO;
    }
}
