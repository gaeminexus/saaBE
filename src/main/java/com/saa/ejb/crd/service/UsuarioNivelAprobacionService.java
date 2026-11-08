package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.UsuarioNivelAprobacion;

import jakarta.ejb.Local;

@Local
public interface UsuarioNivelAprobacionService extends EntityService<UsuarioNivelAprobacion> {
    /**
     * Agrega (código nulo) o cambia el registro con sus validaciones de configuración (DISENO B4).
     *
     * @param usuario quien registra; tiene que existir en el sistema
     * @throws com.saa.basico.util.IncomeException con el motivo (la capa REST lo responde 422)
     */
    UsuarioNivelAprobacion guardar(UsuarioNivelAprobacion registro, String usuario) throws Throwable;

    /** Lo deja inactivo ({@code ESTD = 0}); no se borra (hay bitácora que lo referencia). */
    UsuarioNivelAprobacion inactivar(Long id, String usuario) throws Throwable;

}
