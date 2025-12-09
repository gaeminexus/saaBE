package com.saa.ejb.asoprep.service;

import java.io.InputStream;
import java.util.List;

import com.saa.model.credito.CargaArchivo;
import com.saa.model.credito.DetalleCargaArchivo;
import com.saa.model.credito.ParticipeXCargaArchivo;

import jakarta.ejb.Local;

/**
 * Servicio Stateful para procesar archivos Petro con manejo de transacciones
 */
@Local
public interface CargaArchivoPetroService {

    /**
     * Método principal que procesa el archivo Petro y los datos relacionados
     * 
     * @param archivoInputStream : Stream del archivo
     * @param fileName : Nombre del archivo
     * @param cargaArchivo : Registro de CargaArchivo
     * @param detallesCargaArchivos : Lista de detalles de carga
     * @param participesXCargaArchivo : Lista de partícipes por carga
     * @return Ruta donde se almacenó el archivo
     * @throws Throwable : Excepción
     */
    String procesarArchivoPetro(InputStream archivoInputStream, String fileName, 
                               CargaArchivo cargaArchivo, 
                               List<DetalleCargaArchivo> detallesCargaArchivos,
                               List<ParticipeXCargaArchivo> participesXCargaArchivo) throws Throwable;
    
    /**
     * Metodo para validar el archivo de Petro Comercial
     * 
     * @param archivoInputStream : Stream del archivo
     * @param fileName : Nombre del archivo
     * @param cargaArchivo : Registro de CargaArchivo
     * @return Ruta donde se almacenó el archivo
     * @throws Throwable : Excepción
     */
    CargaArchivo validarArchivoPetro(InputStream archivoInputStream, String fileName, CargaArchivo cargaArchivo) throws Throwable;
    
}
