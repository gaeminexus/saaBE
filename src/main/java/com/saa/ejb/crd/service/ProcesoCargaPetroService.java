package com.saa.ejb.crd.service;

import com.saa.model.crd.CargaArchivo;

import jakarta.ejb.Local;

/**
 * Servicio para el procesamiento de archivos Petro FASE 2
 * Cruza la información del archivo con préstamos y aportes
 */
@Local
public interface ProcesoCargaPetroService {

    /**
     * Procesa un archivo de carga Petro ya validado (FASE 1)
     * Cruza la información con préstamos y aportes del sistema
     * 
     * @param idCargaArchivo ID del CargaArchivo a procesar
     * @return CargaArchivo procesado con estadísticas
     * @throws Throwable Si ocurre algún error
     */
    CargaArchivo procesarCargaPetro(Long idCargaArchivo) throws Throwable;

}
