package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.ValorPagoPensionComplementaria;

import jakarta.ejb.Local;

@Local
public interface ValorPagoPensionComplementariaService extends EntityService<ValorPagoPensionComplementaria> {

    /**
     * Para G44 — Retorna los registros VPPC asociados a una entidad.
     * De aquí se obtiene el valorPagar para llenar valorPension y valorNetoRecibir.
     *
     * @param codigoEntidad Código de la entidad (CRD.ENTD)
     * @return Lista de ValorPagoPensionComplementaria para esa entidad
     */
    List<ValorPagoPensionComplementaria> selectByEntidad(Long codigoEntidad) throws Throwable;

    /**
     * Carga VPPC para múltiples entidades en una sola consulta.
     * Optimización para evitar N+1 queries.
     */
    List<ValorPagoPensionComplementaria> selectByEntidadesIn(List<Long> codigosEntidades) throws Throwable;
}

