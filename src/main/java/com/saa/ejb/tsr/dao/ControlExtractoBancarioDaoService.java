/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial").
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.ControlExtractoBancario;
import com.saa.model.tsr.CuentaBancaria;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Service ControlExtractoBancario.
 */
@Local
public interface ControlExtractoBancarioDaoService extends EntityDao<ControlExtractoBancario> {

    /**
     * Busca el resumen de control de una empresa para un mes/anio especifico.
     * @param idEmpresa : Id de la empresa
     * @param mes       : Mes (1-12)
     * @param anio      : Anio
     * @return          : ControlExtractoBancario si ya existe, null si no
     * @throws Throwable : Excepcion
     */
    ControlExtractoBancario selectByEmpresaYPeriodo(Long idEmpresa, Long mes, Long anio) throws Throwable;

    /**
     * Recupera los codigos (CNBCCDGO) de las cuentas bancarias activas de una empresa,
     * a traves de la relacion CuentaBancaria -> Banco -> Empresa.
     * @param idEmpresa : Id de la empresa
     * @return          : Listado de codigos de cuenta bancaria activos
     * @throws Throwable : Excepcion
     */
    List<Long> selectCuentasActivasPorEmpresa(Long idEmpresa) throws Throwable;

    /**
     * Cuenta, dentro de una lista de cuentas, cuantas ya tienen una Conciliacion
     * concluida (rubro EstadosConciliacion.CONCILIADO) para el periodo indicado.
     * Fuente interina de "conciliadas" hasta que exista el motor de conciliacion
     * DEXB-MVCB descrito en el documento de diseño.
     * @param idsCuenta : Lista de codigos de cuenta bancaria a verificar
     * @param idPeriodo : Id del periodo contable
     * @return          : Cantidad de cuentas con conciliacion concluida en ese periodo
     * @throws Throwable : Excepcion
     */
    Long contarCuentasConciliadas(List<Long> idsCuenta, Long idPeriodo) throws Throwable;

    /**
     * Recupera las entidades completas (con Banco y numeroCuenta ya cargados)
     * de las cuentas bancarias activas de una empresa - version de
     * selectCuentasActivasPorEmpresa que trae el objeto completo en vez de
     * solo el codigo, para mostrar nombre de banco y numero de cuenta en el
     * drill-down del tablero de cumplimiento.
     * @param idEmpresa : Id de la empresa
     * @return          : Cuentas bancarias activas
     * @throws Throwable : Excepcion
     */
    List<CuentaBancaria> selectCuentasBancariasActivas(Long idEmpresa) throws Throwable;

    /**
     * De una lista de cuentas, devuelve los codigos de las que ya tienen una
     * Conciliacion concluida (rubro EstadosConciliacion.CONCILIADO) para el
     * periodo indicado - version de contarCuentasConciliadas que devuelve el
     * conjunto de ids en vez de solo el conteo.
     * @param idsCuenta : Lista de codigos de cuenta bancaria a verificar
     * @param idPeriodo : Id del periodo contable
     * @return          : Codigos de cuenta bancaria conciliados en ese periodo
     * @throws Throwable : Excepcion
     */
    List<Long> selectCuentasConciliadas(List<Long> idsCuenta, Long idPeriodo) throws Throwable;

    /**
     * Ids de Periodo ya cerrados (cerrado = 1) para una empresa.
     * @param idEmpresa : Id de la empresa
     * @return          : Ids de Periodo cerrados
     * @throws Throwable : Excepcion
     */
    List<Long> selectPeriodosCerrados(Long idEmpresa) throws Throwable;

}
