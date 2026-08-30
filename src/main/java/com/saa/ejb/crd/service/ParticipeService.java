package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Participe;

import jakarta.ejb.Local;

@Local
public interface ParticipeService extends EntityService<Participe> {

    /**
     * Recupera el partícipe asociado a una entidad específica.
     * @param codigoEntidad : Código de la entidad
     * @return : Listado de partícipes
     * @throws Throwable : Excepcion
     */
    List<Participe> selectByEntidad(Long codigoEntidad) throws Throwable;

    /**
     * Igual que {@link #saveSingle(Participe)}, pero además sella
     * {@code EntidadService.sellarActualizacion} sobre ENTD en la misma transacción
     * (pedido 9, pantalla de actualización de datos del partícipe).
     *
     * @param participe : Participe a guardar
     * @param usuario   : Usuario que hace el cambio (puede ser null)
     * @throws Throwable : Excepcion
     */
    Participe saveSingle(Participe participe, String usuario) throws Throwable;
}
