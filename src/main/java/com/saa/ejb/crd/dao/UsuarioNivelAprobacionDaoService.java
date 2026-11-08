package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.UsuarioNivelAprobacion;

import jakarta.ejb.Local;

@Local
public interface UsuarioNivelAprobacionDaoService extends EntityDao<UsuarioNivelAprobacion> {
    /** Usuarios ACTIVOS de un nivel. */
    List<UsuarioNivelAprobacion> selectActivosByNivel(Long idNivel) throws Throwable;

}
