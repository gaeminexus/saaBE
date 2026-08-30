package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.ReferenciaPersonal;

import jakarta.ejb.Local;

@Local
public interface ReferenciaPersonalService extends EntityService<ReferenciaPersonal> {

    /**
     * Retorna las referencias personales de una entidad.
     * @param idEntidad código de la entidad
     * @return lista de referencias personales
     */
    List<ReferenciaPersonal> selectByParent(Long idEntidad) throws Throwable;

    /**
     * Igual que {@link #saveSingle(ReferenciaPersonal)}, pero además sella
     * {@code EntidadService.sellarActualizacion} sobre ENTD en la misma transacción
     * (pedido 9, pantalla de actualización de datos del partícipe).
     *
     * @param referencia : ReferenciaPersonal a guardar
     * @param usuario    : Usuario que hace el cambio (puede ser null)
     * @throws Throwable : Excepcion
     */
    ReferenciaPersonal saveSingle(ReferenciaPersonal referencia, String usuario) throws Throwable;
}
