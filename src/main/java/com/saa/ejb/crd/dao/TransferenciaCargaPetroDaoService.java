/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.TransferenciaCargaPetro;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad TransferenciaCargaPetro (CRD.TRCR): las
 *         transferencias con las que Petro/ARCH pagó una carga.
 */
@Local
public interface TransferenciaCargaPetroDaoService extends EntityDao<TransferenciaCargaPetro> {

    /**
     * Transferencias VIGENTES (no anuladas) de una carga, ordenadas por código.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @return           : Listado; VACÍO si no se ha registrado ninguna
     * @throws Throwable : Excepcion
     */
    List<TransferenciaCargaPetro> selectVigentesByCarga(Long idCarga) throws Throwable;

    /**
     * Todas las transferencias de una carga (vigentes y anuladas), para la pantalla de
     * revisión — el usuario tiene que poder ver qué se anuló y por qué.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @return           : Listado completo; VACÍO si no se ha registrado ninguna
     * @throws Throwable : Excepcion
     */
    List<TransferenciaCargaPetro> selectByCarga(Long idCarga) throws Throwable;

    /**
     * Suma de {@code valor} de las transferencias VIGENTES de una carga. Es el total del
     * asiento transitorio del paso 1, y lo que se valida contra el total del archivo.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @return           : Suma redondeada a 2 decimales; 0.0 si no hay transferencias
     * @throws Throwable : Excepcion
     */
    double sumaValorVigentesByCarga(Long idCarga) throws Throwable;

    /**
     * Empresa contable de una carga Petro, a partir de sus transferencias VIGENTES —
     * primera transferencia con {@code cuentaBancaria.planCuenta.empresa} resuelta.
     *
     * <p>ÚNICA definición del proyecto (2026-09-02): antes vivía privada y duplicada en
     * {@code CobroPetroContableServiceImpl.resolverEmpresa}; ahora ese método delega acá.
     * Cualquier lectura que necesite la empresa de una carga (asiento de aplicación, cuadre de
     * bandas, resolución de cuenta contable para la auditoría) tiene que pasar por este único
     * lugar — dos implementaciones de la misma resolución divergen.</p>
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @return           : Código de la empresa (SCP.PJRQ)
     * @throws Throwable : {@code IncomeException} si ninguna transferencia vigente tiene
     *                     cuenta bancaria con cuenta contable y empresa asignadas
     */
    Long resolverEmpresaByCarga(Long idCarga) throws Throwable;

    /**
     * Misma resolución que {@link #resolverEmpresaByCarga}, pero sobre una lista de
     * transferencias YA LEÍDA — para el llamador que ya tiene {@code selectVigentesByCarga} en
     * la mano y no necesita una segunda consulta a la base para lo mismo.
     *
     * @throws Throwable : {@code IncomeException} si ninguna trae cuenta bancaria con cuenta
     *                     contable y empresa asignadas
     */
    Long resolverEmpresa(List<TransferenciaCargaPetro> vigentes) throws Throwable;
}
