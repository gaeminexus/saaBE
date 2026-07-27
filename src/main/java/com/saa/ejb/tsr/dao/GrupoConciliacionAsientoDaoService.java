/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.tsr.GrupoConciliacionAsiento;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Service GrupoConciliacionAsiento.
 */
@Local
public interface GrupoConciliacionAsientoDaoService extends EntityDao<GrupoConciliacionAsiento> {

    /**
     * Recupera los enlaces (filas de DetalleAsiento) de un grupo.
     * @param idGrupo    : Id del GrupoConciliacionContable
     * @return           : Enlaces del grupo
     * @throws Throwable : Excepcion
     */
    List<GrupoConciliacionAsiento> selectByGrupo(Long idGrupo) throws Throwable;

    /**
     * Filas de DetalleAsiento de la cuenta contable del banco (dentro del
     * rango de fechas del período) que todavía no pertenecen a ningún grupo
     * ACTIVO - candidatas para conciliar. Se identifica por PlanCuenta +
     * rango de fechas (igual que DetalleAsientoService.selectByEmpresaCuentaFechas),
     * no por un FK directo a Periodo, porque DetalleAsiento no tiene ese FK.
     * @param idPlanCuenta : Id de la cuenta contable asociada a la cuenta bancaria
     * @param idEmpresa    : Id de la empresa
     * @param primerDia    : Primer dia del periodo
     * @param ultimoDia    : Ultimo dia del periodo
     * @return              : Filas pendientes de conciliar
     * @throws Throwable   : Excepcion
     */
    List<DetalleAsiento> selectPendientes(Long idPlanCuenta, Long idEmpresa, LocalDate primerDia, LocalDate ultimoDia)
            throws Throwable;

    /**
     * Cuenta las filas pendientes (ver selectPendientes), sin traerlas.
     * @param idPlanCuenta : Id de la cuenta contable asociada a la cuenta bancaria
     * @param idEmpresa    : Id de la empresa
     * @param primerDia    : Primer dia del periodo
     * @param ultimoDia    : Ultimo dia del periodo
     * @return              : Cantidad de filas pendientes
     * @throws Throwable   : Excepcion
     */
    Long contarPendientes(Long idPlanCuenta, Long idEmpresa, LocalDate primerDia, LocalDate ultimoDia)
            throws Throwable;

    /**
     * De la lista de ids de DetalleAsiento indicados, devuelve cuáles ya
     * pertenecen a un grupo ACTIVO.
     * @param idsDetalleAsiento : Ids de DetalleAsiento a verificar
     * @return                  : Subconjunto ya conciliado (activo)
     * @throws Throwable        : Excepcion
     */
    List<Long> selectIdsEnGrupoActivo(List<Long> idsDetalleAsiento) throws Throwable;

}
