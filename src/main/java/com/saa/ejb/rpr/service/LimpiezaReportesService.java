package com.saa.ejb.rpr.service;

import jakarta.ejb.Local;

@Local
public interface LimpiezaReportesService {
    void limpiarDatosReportes(Long codigoEjecucion);
}
