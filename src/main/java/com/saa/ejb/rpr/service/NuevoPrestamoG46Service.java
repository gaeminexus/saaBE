package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.NuevoPrestamoG46;

import jakarta.ejb.Local;

@Local
public interface NuevoPrestamoG46Service extends EntityService<NuevoPrestamoG46> {

    List<NuevoPrestamoG46> selectByDetalle(Long codigoDetalle) throws Throwable;
}
