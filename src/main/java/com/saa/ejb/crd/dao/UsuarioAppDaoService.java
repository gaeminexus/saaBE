/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.UsuarioApp;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad UsuarioApp (CRD.USAP): credenciales de la app
 *         móvil ASOPREP.
 */
@Local
public interface UsuarioAppDaoService extends EntityDao<UsuarioApp> {

    /**
     * Busca la credencial por identificación (cédula) — el login de la app.
     *
     * @param identificacion : Identificación a buscar
     * @return                : La credencial encontrada, o {@code null} si no existe
     * @throws Throwable     : Excepcion
     */
    UsuarioApp selectByIdentificacion(String identificacion) throws Throwable;

    /**
     * Busca la credencial de un partícipe por su código de Entidad (relación 1:1).
     *
     * @param idEntidad : Código de la entidad (CRD.ENTD)
     * @return          : La credencial encontrada, o {@code null} si no existe
     * @throws Throwable : Excepcion
     */
    UsuarioApp selectByEntidad(Long idEntidad) throws Throwable;
}
