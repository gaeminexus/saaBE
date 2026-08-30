package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.ReferenciaFamiliar;

import jakarta.ejb.Local;

@Local
public interface ReferenciaFamiliarService extends EntityService<ReferenciaFamiliar> {

    /**
     * Retorna las referencias familiares de una entidad.
     * @param idEntidad código de la entidad
     * @return lista de referencias familiares
     */
    List<ReferenciaFamiliar> selectByParent(Long idEntidad) throws Throwable;

    /**
     * Igual que {@link #saveSingle(ReferenciaFamiliar)}, pero además sella
     * {@code EntidadService.sellarActualizacion} sobre ENTD en la misma transacción
     * (pedido 9, pantalla de actualización de datos del partícipe).
     *
     * @param referencia : ReferenciaFamiliar a guardar
     * @param usuario    : Usuario que hace el cambio (puede ser null)
     * @throws Throwable : Excepcion
     */
    ReferenciaFamiliar saveSingle(ReferenciaFamiliar referencia, String usuario) throws Throwable;
}
