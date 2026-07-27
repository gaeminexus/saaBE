/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.GrupoConciliacionAsiento;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio CRUD de GrupoConciliacionAsiento (enlace grupo ↔
 * DetalleAsiento).</p>
 */
@Local
public interface GrupoConciliacionAsientoService extends EntityService<GrupoConciliacionAsiento> {

    /**
     * Recupera los enlaces de un grupo.
     * @param idGrupo    : Id del GrupoConciliacionContable
     * @return           : Enlaces del grupo
     * @throws Throwable : Excepcion
     */
    List<GrupoConciliacionAsiento> selectByGrupo(Long idGrupo) throws Throwable;

}
