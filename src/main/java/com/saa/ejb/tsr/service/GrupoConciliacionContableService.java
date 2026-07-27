/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.GrupoConciliacionContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio CRUD de GrupoConciliacionContable. La lógica de negocio de
 * crear/deshacer un grupo (validaciones de monto y fecha) vive en
 * {@link ConciliacionContableMatchService}, no aquí.</p>
 */
@Local
public interface GrupoConciliacionContableService extends EntityService<GrupoConciliacionContable> {

    /**
     * Recupera los grupos activos de una cabecera de conciliación contable.
     * @param idConciliacionContable : Id de la cabecera
     * @return                       : Grupos activos
     * @throws Throwable             : Excepcion
     */
    List<GrupoConciliacionContable> selectActivosByConciliacion(Long idConciliacionContable) throws Throwable;

}
