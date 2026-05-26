package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.InformacionGeneralFondo;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface InformacionGeneralFondoDaoService extends EntityDao<InformacionGeneralFondo> {

    /**
     * Recupera los registros del fondo con estado = 2 (Modificado).
     * Se usan para determinar si se debe generar el G40.
     * @return : Listado de registros modificados
     * @throws Throwable : Excepcion
     */
    List<InformacionGeneralFondo> selectModificados() throws Throwable;
}
