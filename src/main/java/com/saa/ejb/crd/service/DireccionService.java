package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Direccion;

import jakarta.ejb.Local;

@Local
public interface DireccionService extends EntityService<Direccion> {

    /**
     * Para G45 — Retorna las direcciones de una entidad.
     * De la primera dirección se obtiene parroquia.nombre → parroquia del G45.
     */
    List<Direccion> selectByParent(Long codigoEntidad) throws Throwable;

    /**
     * Igual que {@link #saveSingle(Direccion)}, pero además sella
     * {@code EntidadService.sellarActualizacion} sobre ENTD en la misma transacción
     * (pedido 9, pantalla de actualización de datos del partícipe).
     *
     * @param direccion : Direccion a guardar
     * @param usuario   : Usuario que hace el cambio (puede ser null)
     * @throws Throwable : Excepcion
     */
    Direccion saveSingle(Direccion direccion, String usuario) throws Throwable;
}

