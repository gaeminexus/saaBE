package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ValorPagoPensionComplementaria;

import jakarta.ejb.Local;

@Local
public interface ValorPagoPensionComplementariaDaoService extends EntityDao<ValorPagoPensionComplementaria> {

    /**
     * Para G44 — Retorna los registros de VPPC asociados a una entidad.
     * De aquí se obtiene el valorPagar para llenar valorPension y valorNetoRecibir.
     *
     * @param codigoEntidad Código de la entidad (CRD.ENTD)
     * @return Lista de ValorPagoPensionComplementaria para esa entidad
     */
    List<ValorPagoPensionComplementaria> selectByEntidad(Long codigoEntidad) throws Throwable;
}
