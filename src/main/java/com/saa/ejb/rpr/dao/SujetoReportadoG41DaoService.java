package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.model.rpr.SujetoReportadoG41;

import jakarta.ejb.Local;

/** D-038 — acceso a RPR.SG41, la historia del estado de los partícipes reportado en la G41. */
@Local
public interface SujetoReportadoG41DaoService {

    /** Filas con período ANTERIOR al dado (estado previo de cada sujeto), ordenadas por período ascendente. */
    List<SujetoReportadoG41> selectAnterioresA(Long periodo) throws Throwable;

    /** Filas del período dado (cualquier origen). */
    List<SujetoReportadoG41> selectDelPeriodo(Long periodo) throws Throwable;

    /** Cuántas filas generadas por el SAA hay en períodos POSTERIORES al dado (meses que habría que regenerar). */
    long countEjecucionPosterioresA(Long periodo) throws Throwable;

    /** Borra lo que una ejecución anterior registró para el período (regenerar un mes sin duplicar). */
    int deleteEjecucionDelPeriodo(Long periodo) throws Throwable;

    void insertar(SujetoReportadoG41 sujeto) throws Throwable;

    /** Identificaciones de la G43 (cesantes) del EJRD. */
    List<String> selectIdentificacionesG43(Long idEjrdG43) throws Throwable;

    /** Identificaciones de la G44 (jubilados) del EJRD. */
    List<String> selectIdentificacionesG44(Long idEjrdG44) throws Throwable;
}
