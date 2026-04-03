package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.AfectacionValoresParticipeCarga;

import jakarta.ejb.Local;

@Local
public interface AfectacionValoresParticipeCargaDaoService extends EntityDao<AfectacionValoresParticipeCarga> {
    
    /**
     * Busca todas las afectaciones de una novedad específica
     * @param codigoNovedad Código de la NovedadParticipeCarga
     * @return Lista de afectaciones encontradas
     * @throws Throwable Si ocurre un error
     */
    List<AfectacionValoresParticipeCarga> selectByNovedad(Long codigoNovedad) throws Throwable;
    
    /**
     * Busca todas las afectaciones de un préstamo específico
     * @param codigoPrestamo Código del Prestamo
     * @return Lista de afectaciones encontradas
     * @throws Throwable Si ocurre un error
     */
    List<AfectacionValoresParticipeCarga> selectByPrestamo(Long codigoPrestamo) throws Throwable;
    
    /**
     * Busca todas las afectaciones de una cuota específica
     * @param codigoCuota Código del DetallePrestamo
     * @return Lista de afectaciones encontradas
     * @throws Throwable Si ocurre un error
     */
    List<AfectacionValoresParticipeCarga> selectByCuota(Long codigoCuota) throws Throwable;
    
    /**
     * Busca una afectación específica por código de novedad y código de cuota
     * @param codigoNovedad Código de la NovedadParticipeCarga
     * @param codigoDetallePrestamo Código del DetallePrestamo
     * @return AfectacionValoresParticipeCarga encontrada o null
     * @throws Throwable Si ocurre un error
     */
    AfectacionValoresParticipeCarga selectByNovedadYCuota(Long codigoNovedad, Long codigoDetallePrestamo) throws Throwable;
}
