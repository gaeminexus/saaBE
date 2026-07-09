package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.InformacionGeneralFondo;

import jakarta.ejb.Local;

@Local
public interface InformacionGeneralFondoService extends EntityService<InformacionGeneralFondo> {

    /**
     * Recupera los registros del fondo con estado = 2 (Modificado).
     * @return : Listado de registros modificados
     * @throws Throwable : Excepcion
     */
    List<InformacionGeneralFondo> selectModificados() throws Throwable;
}
