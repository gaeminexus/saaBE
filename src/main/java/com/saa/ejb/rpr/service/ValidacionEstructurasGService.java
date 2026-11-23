package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.ejb.rpr.service.dto.ObservacionValidacionG;

import jakarta.ejb.Local;

/**
 * C39 (23-nov-2026) — validación previa de las estructuras G40–G51 de una ejecución, con las reglas de los manuales de
 * la Superintendencia (Catastros FCPC v1.2, Operaciones de Crédito FCPC v2.4) que motivaron rechazos en junio 2025:
 * campos obligatorios vacíos o con fechas de relleno (G41, G45, G46), sujetos de la G46 que no constan en la G45,
 * operaciones de la G48 sin alta, cancelaciones de la G49 que no estaban en la G48 anterior, altas de la G46 sin su
 * garantía en la G51. Solo lee.
 */
@Local
public interface ValidacionEstructurasGService {

    /** Observaciones de la ejecución {@code codigoEjrc}; lista vacía si todo está en orden. */
    List<ObservacionValidacionG> validar(Long codigoEjrc) throws Throwable;

    /** Resumen de una línea ("N error(es): G41 (2), G46 (1)") o "sin errores", para las observaciones de la ejecución. */
    String resumen(List<ObservacionValidacionG> observaciones);
}
