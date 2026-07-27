/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.model.tsr.GrupoConciliacionExtracto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Service GrupoConciliacionExtracto.
 */
@Local
public interface GrupoConciliacionExtractoDaoService extends EntityDao<GrupoConciliacionExtracto> {

    /**
     * Recupera los enlaces (filas de DetalleExtractoBancario) de un grupo.
     * @param idGrupo    : Id del GrupoConciliacionContable
     * @return           : Enlaces del grupo
     * @throws Throwable : Excepcion
     */
    List<GrupoConciliacionExtracto> selectByGrupo(Long idGrupo) throws Throwable;

    /**
     * Filas de DetalleExtractoBancario de una cuenta/período que todavía no
     * pertenecen a ningún grupo ACTIVO - candidatas para conciliar.
     * @param idCuentaBancaria : Id de la cuenta bancaria
     * @param idPeriodo        : Id del periodo contable
     * @return                 : Filas pendientes de conciliar
     * @throws Throwable       : Excepcion
     */
    List<DetalleExtractoBancario> selectPendientes(Long idCuentaBancaria, Long idPeriodo) throws Throwable;

    /**
     * Cuenta las filas pendientes (ver selectPendientes), sin traerlas.
     * @param idCuentaBancaria : Id de la cuenta bancaria
     * @param idPeriodo        : Id del periodo contable
     * @return                 : Cantidad de filas pendientes
     * @throws Throwable       : Excepcion
     */
    Long contarPendientes(Long idCuentaBancaria, Long idPeriodo) throws Throwable;

    /**
     * De la lista de ids de DetalleExtractoBancario indicados, devuelve
     * cuáles ya pertenecen a un grupo ACTIVO (para rechazar la creación de un
     * grupo nuevo que los reutilice).
     * @param idsDetalleExtracto : Ids de DetalleExtractoBancario a verificar
     * @return                   : Subconjunto ya conciliado (activo)
     * @throws Throwable         : Excepcion
     */
    List<Long> selectIdsEnGrupoActivo(List<Long> idsDetalleExtracto) throws Throwable;

}
