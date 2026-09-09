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

    /**
     * Todos los enlaces GCEX de una lista de ids de DetalleExtractoBancario, en CUALQUIER
     * estado de su grupo (a diferencia de {@link #selectIdsEnGrupoActivo}, que solo mira
     * grupos activos). GCEX no tiene su propio estado -- solo lo tiene GRCC, su padre -- asi
     * que un enlace de un grupo ya DESHECHO sigue existiendo para siempre y sigue siendo una
     * FK hacia DEXB ({@code ConciliacionContableMatchServiceImpl.deshacerGrupo} desactiva el
     * GRCC pero nunca borra sus filas GCEX). Usado por
     * ImportacionExtractoBancarioServiceImpl.recargar para borrar en cascada, ANTES del DEXB,
     * los enlaces de grupos ya deshechos -- son peso muerto, no historia con valor: el GRCC
     * inactivo ya queda como registro de que la conciliacion existio y se deshizo, sin
     * necesidad de conservar el enlace puntual a una fila de extracto que el usuario decidio
     * borrar. Los enlaces de un grupo ACTIVO nunca llegan a este metodo: ese caso ya lo
     * bloquea {@link #selectIdsEnGrupoActivo} antes.
     * @param idsDetalleExtracto : Ids de TSR.DEXB a verificar
     * @return                   : Enlaces GCEX (cualquier estado de grupo) para esos ids
     * @throws Throwable         : Excepcion
     */
    List<GrupoConciliacionExtracto> selectPorDetalleExtracto(List<Long> idsDetalleExtracto) throws Throwable;

}
