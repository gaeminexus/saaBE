/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial").
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.DetalleExtractoBancario;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Service DetalleExtractoBancario.
 */
@Local
public interface DetalleExtractoBancarioDaoService extends EntityDao<DetalleExtractoBancario> {

    /**
     * Busca una fila ya cargada para la misma cuenta con el mismo hash de deduplicacion
     * (control de reimportacion de archivos con rango de fechas solapado).
     * @param idCuenta : Id de la cuenta bancaria
     * @param hash     : Hash de deduplicacion de la fila
     * @return         : DetalleExtractoBancario si ya existe, null si no
     * @throws Throwable : Excepcion
     */
    DetalleExtractoBancario selectByCuentaYHash(Long idCuenta, String hash) throws Throwable;

}
