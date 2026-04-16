package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.ParticipeDetalleGeneracionArchivo;

import jakarta.ejb.Local;

/**
 * Interface Service para ParticipeDetalleGeneracionArchivo (PDGA).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Local
public interface ParticipeDetalleGeneracionArchivoService extends EntityService<ParticipeDetalleGeneracionArchivo> {

    /**
     * Crea un nuevo registro de partícipe.
     * 
     * @param participeDetalle Objeto con los datos del partícipe
     * @return Registro creado con ID asignado
     * @throws Throwable Si ocurre un error
     */
    ParticipeDetalleGeneracionArchivo crear(ParticipeDetalleGeneracionArchivo participeDetalle) throws Throwable;
    
    /**
     * Actualiza un registro existente.
     * 
     * @param participeDetalle Objeto con los datos actualizados
     * @return Registro actualizado
     * @throws Throwable Si ocurre un error
     */
    ParticipeDetalleGeneracionArchivo actualizar(ParticipeDetalleGeneracionArchivo participeDetalle) throws Throwable;
    
    /**
     * Busca un registro por ID.
     * 
     * @param codigo Código del registro
     * @return Registro encontrado o null
     * @throws Throwable Si ocurre un error
     */
    ParticipeDetalleGeneracionArchivo buscarPorId(Long codigo) throws Throwable;
    
    /**
     * Lista todos los partícipes de un detalle de generación.
     * 
     * @param codigoDetalle Código del detalle (DTGA)
     * @return Lista de partícipes ordenados por número de línea
     * @throws Throwable Si ocurre un error
     */
    List<ParticipeDetalleGeneracionArchivo> listarPorDetalle(Long codigoDetalle) throws Throwable;
    
    /**
     * Lista todos los registros de un partícipe.
     * 
     * @param codigoEntidad Código de la entidad (partícipe)
     * @return Lista de registros
     * @throws Throwable Si ocurre un error
     */
    List<ParticipeDetalleGeneracionArchivo> listarPorEntidad(Long codigoEntidad) throws Throwable;
    
    /**
     * Lista todos los registros de una generación completa.
     * 
     * @param codigoGeneracion Código de la generación (GNAP)
     * @return Lista completa ordenada por tipo de producto y número de línea
     * @throws Throwable Si ocurre un error
     */
    List<ParticipeDetalleGeneracionArchivo> listarPorGeneracion(Long codigoGeneracion) throws Throwable;
}
