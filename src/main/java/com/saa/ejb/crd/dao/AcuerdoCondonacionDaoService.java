/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.AcuerdoCondonacion;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad AcuerdoCondonacion (CRD.ACCN).
 */
@Local
public interface AcuerdoCondonacionDaoService extends EntityDao<AcuerdoCondonacion> {

    /**
     * Bandeja de un estado puntual (rubro {@link com.saa.rubros.CrdEstadoAcuerdoCondonacion}).
     *
     * @param estado     : Estado a filtrar
     * @return           : Listado ordenado por fecha de registro ascendente; VACÍO si no hay
     * @throws Throwable : Excepcion
     */
    List<AcuerdoCondonacion> selectByEstado(Long estado) throws Throwable;

    /**
     * Los acuerdos de un préstamo, para su ficha. Como la tabla es la ÚNICA fuente de cuánto
     * se condonó (K6: el préstamo queda CANCELADO, indistinguible de uno pagado), esta
     * consulta es la que responde "¿este préstamo tuvo un acuerdo?".
     *
     * @param idPrestamo : Código del préstamo (CRD.PRST)
     * @return           : Listado; VACÍO si el préstamo nunca tuvo un acuerdo
     * @throws Throwable : Excepcion
     */
    List<AcuerdoCondonacion> selectByPrestamo(Long idPrestamo) throws Throwable;

    /**
     * Los acuerdos de una entidad (partícipe), para su ficha.
     *
     * @param idEntidad  : Código de la entidad (CRD.ENTD)
     * @return           : Listado; VACÍO si no tiene ningún acuerdo
     * @throws Throwable : Excepcion
     */
    List<AcuerdoCondonacion> selectByEntidad(Long idEntidad) throws Throwable;

    /**
     * El acuerdo enlazado a un cobro de CRD.CBCR — para la anulación en cascada
     * ({@code CobroCreditoService#anularCobro} → {@code AcuerdoCondonacionService#anularAcuerdoPorCobro}).
     *
     * @param idCobro    : Código del cobro (CRD.CBCR)
     * @return           : El acuerdo, o {@code null} si ese cobro no es de un acuerdo
     * @throws Throwable : Excepcion
     */
    AcuerdoCondonacion selectByCobroCredito(Long idCobro) throws Throwable;
}
