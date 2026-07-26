/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial").
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.ExtractoBancario;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Service ExtractoBancario.
 */
@Local
public interface ExtractoBancarioDaoService extends EntityDao<ExtractoBancario> {

    /**
     * Busca un extracto por el hash SHA-256 de su archivo origen (control de duplicados).
     * @param hash : SHA-256 del archivo
     * @return      : ExtractoBancario si ya existe, null si no
     * @throws Throwable : Excepcion
     */
    ExtractoBancario selectByHash(String hash) throws Throwable;

    /**
     * Recupera los extractos de una cuenta bancaria cuyo rango de fechas
     * se solapa con el periodo indicado. Usado para saber si un mes ya
     * quedo cubierto (CTEB) y para el drill-down de cuentas pendientes.
     * @param idCuenta   : Id de la cuenta bancaria
     * @param primerDia  : Primer dia del periodo a verificar
     * @param ultimoDia  : Ultimo dia del periodo a verificar
     * @return           : Listado de extractos que se solapan con el periodo
     * @throws Throwable : Excepcion
     */
    List<ExtractoBancario> selectByCuentaYCobertura(Long idCuenta, LocalDate primerDia, LocalDate ultimoDia) throws Throwable;

    /**
     * Recupera los codigos de cuenta bancaria, dentro de una lista de cuentas activas,
     * que ya tienen al menos un extracto solapando el periodo indicado.
     * @param idsCuenta  : Lista de codigos de cuenta bancaria a verificar
     * @param primerDia  : Primer dia del periodo
     * @param ultimoDia  : Ultimo dia del periodo
     * @return           : Listado de codigos de cuenta bancaria con cobertura
     * @throws Throwable : Excepcion
     */
    List<Long> selectCuentasConCobertura(List<Long> idsCuenta, LocalDate primerDia, LocalDate ultimoDia) throws Throwable;

}
