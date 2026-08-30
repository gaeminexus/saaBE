package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Conyuge;

import jakarta.ejb.Local;

@Local
public interface ConyugeService extends EntityService<Conyuge> {

    /**
     * Retorna el cónyuge asociado a una entidad.
     * @param idEntidad código de la entidad
     * @return lista (máximo 1 registro)
     */
    List<Conyuge> selectByParent(Long idEntidad) throws Throwable;

    /**
     * Igual que {@link #saveSingle(Conyuge)}, pero además sella
     * {@code EntidadService.sellarActualizacion} sobre ENTD en la misma transacción
     * (pedido 9, pantalla de actualización de datos del partícipe).
     *
     * @param conyuge : Conyuge a guardar
     * @param usuario : Usuario que hace el cambio (puede ser null)
     * @throws Throwable : Excepcion
     */
    Conyuge saveSingle(Conyuge conyuge, String usuario) throws Throwable;
}
