/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.GrupoConciliacionExtracto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio CRUD de GrupoConciliacionExtracto (enlace grupo ↔
 * DetalleExtractoBancario).</p>
 */
@Local
public interface GrupoConciliacionExtractoService extends EntityService<GrupoConciliacionExtracto> {

    /**
     * Recupera los enlaces de un grupo.
     * @param idGrupo    : Id del GrupoConciliacionContable
     * @return           : Enlaces del grupo
     * @throws Throwable : Excepcion
     */
    List<GrupoConciliacionExtracto> selectByGrupo(Long idGrupo) throws Throwable;

}
