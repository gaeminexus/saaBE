package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.InformacionGeneralFondo;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface InformacionGeneralFondoService extends EntityService<InformacionGeneralFondo> {

    /**
     * Recupera los registros del fondo con estado = 2 (Modificado).
     * @return : Listado de registros modificados
     * @throws Throwable : Excepcion
     */
    List<InformacionGeneralFondo> selectModificados() throws Throwable;
}
