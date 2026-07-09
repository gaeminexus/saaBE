package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Exter;

import jakarta.ejb.Local;

@Local
public interface ExterService extends EntityService<Exter> {

    /**
     * Recupera el registro de Exter por cédula.
     * @param cedula : Número de identificación
     * @return : Listado (0 o 1 elemento)
     * @throws Throwable : Excepcion
     */
    List<Exter> selectByCedula(String cedula) throws Throwable;
}
