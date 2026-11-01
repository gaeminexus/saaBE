package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.model.rpr.SujetoReportadoG45;

import jakarta.ejb.Local;

/** D-035 — acceso a RPR.SG45, el registro de sujetos ya reportados en la G45. */
@Local
public interface SujetoReportadoG45DaoService {

    /** Todo el registro (unos pocos miles de filas). */
    List<SujetoReportadoG45> selectAll() throws Throwable;

    /** Borra lo que una ejecución anterior registró para el período (regenerar un mes sin duplicar). */
    int deleteEjecucionDelPeriodo(Long periodo) throws Throwable;

    void insertar(SujetoReportadoG45 sujeto) throws Throwable;

    SujetoReportadoG45 actualizar(SujetoReportadoG45 sujeto) throws Throwable;

    /** Garantes y codeudores de la G50 del EJRD: {tipoIdentificacionGarante, identificacionGarante}. */
    List<Object[]> selectGarantesG50(Long idEjrdG50) throws Throwable;
}
