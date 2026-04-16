package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.DetalleGeneracionArchivo;

import jakarta.ejb.Local;

/**
 * Interface Service para DetalleGeneracionArchivo (DTGA).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Local
public interface DetalleGeneracionArchivoService extends EntityService<DetalleGeneracionArchivo> {

    /**
     * Crea un nuevo detalle de generación.
     * 
     * @param detalle Objeto con los datos del detalle
     * @return Detalle creado con ID asignado
     * @throws Throwable Si ocurre un error
     */
    DetalleGeneracionArchivo crear(DetalleGeneracionArchivo detalle) throws Throwable;
    
    /**
     * Actualiza un detalle existente.
     * 
     * @param detalle Objeto con los datos actualizados
     * @return Detalle actualizado
     * @throws Throwable Si ocurre un error
     */
    DetalleGeneracionArchivo actualizar(DetalleGeneracionArchivo detalle) throws Throwable;
    
    /**
     * Busca un detalle por ID.
     * 
     * @param codigo Código del detalle
     * @return Detalle encontrado o null
     * @throws Throwable Si ocurre un error
     */
    DetalleGeneracionArchivo buscarPorId(Long codigo) throws Throwable;
    
    /**
     * Lista todos los detalles de una generación.
     * 
     * @param codigoGeneracion Código de la generación (GNAP)
     * @return Lista de detalles por tipo de producto
     * @throws Throwable Si ocurre un error
     */
    List<DetalleGeneracionArchivo> listarPorGeneracion(Long codigoGeneracion) throws Throwable;
    
    /**
     * Busca un detalle específico por generación y producto.
     * 
     * @param codigoGeneracion Código de la generación
     * @param codigoProducto Código del producto (AH, HS, PE, etc.)
     * @return Detalle encontrado o null
     * @throws Throwable Si ocurre un error
     */
    DetalleGeneracionArchivo buscarPorGeneracionYProducto(Long codigoGeneracion, String codigoProducto) throws Throwable;
}
