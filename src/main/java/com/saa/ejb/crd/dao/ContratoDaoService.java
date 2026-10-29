package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Contrato;

import jakarta.ejb.Local;

@Local
public interface ContratoDaoService extends EntityDao<Contrato> {

    /**
     * Todos los contratos de una entidad (activos e inactivos), más reciente primero.
     *
     * @param idEntidad  : Código de la entidad (partícipe)
     * @return           : Contratos de la entidad
     * @throws Throwable : Excepcion
     */
    List<Contrato> selectByEntidad(Long idEntidad) throws Throwable;

    /**
     * El contrato ACTIVO (CNTRESTD = 1) más reciente de una entidad. Null si no tiene
     * ninguno activo.
     *
     * @param idEntidad  : Código de la entidad (partícipe)
     * @return           : El contrato activo, o null
     * @throws Throwable : Excepcion
     */
    Contrato selectActivoPorEntidad(Long idEntidad) throws Throwable;

    /**
     * D-022 (reproceso, cuadre Superintendencia C4): contrato de la entidad VIGENTE A LA FECHA DE
     * CORTE, sin mirar su estado de hoy ({@code CNTRESTD}): {@code CNTRFCIN <= corte} y
     * ({@code CNTRFCTR} nulo o {@code > corte}). Si hay varios, el de inicio más reciente (a igual
     * inicio, el de mayor código). Ver REPROCESO-CARGA-PETRO.md §4.10.
     *
     * @param idEntidad  : Código de la entidad (partícipe)
     * @param corte      : Fecha de corte (último instante del mes de la carga)
     * @return           : El contrato vigente al corte, o null
     * @throws Throwable : Excepcion
     */
    Contrato selectVigenteAlCortePorEntidad(Long idEntidad, java.time.LocalDateTime corte) throws Throwable;

    /**
     * D-022 ampliada (reproceso): tipo de contrato ({@code TPCNCDGO}) más frecuente entre los
     * contratos ACTIVOS ({@code CNTRESTD = 1}) de las entidades con fila AH en la carga dada. Es el
     * que se usa para el contrato que el reproceso crea a un aportante sin ninguno (no hay rubro de
     * tipo de contrato en CRD). Null si no hay ninguno.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @return           : TPCNCDGO más frecuente (a igual frecuencia, el menor), o null
     * @throws Throwable : Excepcion
     */
    Long selectTipoContratoMasFrecuenteAportantesCarga(Long idCarga) throws Throwable;

}
