package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.AprobacionPrestamo;

import jakarta.ejb.Local;

@Local
public interface AprobacionPrestamoDaoService extends EntityDao<AprobacionPrestamo> {
    /** Aprobaciones VIGENTES ({@code APPRESTD = 1}) de un préstamo, por orden de nivel ascendente. */
    List<AprobacionPrestamo> selectVigentesByPrestamo(Long idPrestamo) throws Throwable;

}
