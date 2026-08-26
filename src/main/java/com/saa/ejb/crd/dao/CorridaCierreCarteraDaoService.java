/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CorridaCierreCartera;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad CorridaCierreCartera (CRD.CRCT).
 */
@Local
public interface CorridaCierreCarteraDaoService extends EntityDao<CorridaCierreCartera> {

    /**
     * Corrida VIVA de un período: la que está PREPARADA o EJECUTADA. Las REVERSADAS no
     * cuentan, que es lo que permite reprocesar un mes.
     *
     * Es la consulta de idempotencia: si devuelve algo, ese mes ya se corrió.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @param anio       : Año del mes cerrado
     * @param mes        : Mes cerrado, 1 a 12
     * @return           : La corrida viva, o {@code null} si no hay
     * @throws Throwable : Excepcion
     */
    CorridaCierreCartera selectVivaByPeriodo(Long idEmpresa, Long anio, Long mes) throws Throwable;

    /**
     * Última corrida EJECUTADA anterior al período dado. Es de donde sale el snapshot
     * contra el que se controla la distribución de esta corrida.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @param anio       : Año del período actual
     * @param mes        : Mes del período actual
     * @return           : La corrida ejecutada más reciente anterior, o {@code null}
     * @throws Throwable : Excepcion
     */
    CorridaCierreCartera selectUltimaEjecutadaAntesDe(Long idEmpresa, Long anio, Long mes)
            throws Throwable;

    /**
     * Todas las corridas de una empresa, de la más reciente a la más antigua.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @return           : Listado; VACÍO si nunca se corrió el proceso
     * @throws Throwable : Excepcion
     */
    List<CorridaCierreCartera> selectByEmpresa(Long idEmpresa) throws Throwable;
}
