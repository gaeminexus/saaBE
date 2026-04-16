package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ParticipeDetalleGeneracionArchivo;

import jakarta.ejb.Local;

/**
 * Interface DAO para ParticipeDetalleGeneracionArchivo (PDGA).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Local
public interface ParticipeDetalleGeneracionArchivoDaoService extends EntityDao<ParticipeDetalleGeneracionArchivo> {

    /**
     * Busca todos los partícipes de un detalle de generación.
     * 
     * @param codigoDetalle Código del detalle de generación (DTGA)
     * @return Lista de partícipes ordenados por número de línea
     * @throws Throwable Si ocurre un error
     */
    List<ParticipeDetalleGeneracionArchivo> selectByDetalle(Long codigoDetalle) throws Throwable;
    
    /**
     * Busca todos los registros de un partícipe específico.
     * 
     * @param codigoEntidad Código de la entidad (partícipe)
     * @return Lista de registros del partícipe
     * @throws Throwable Si ocurre un error
     */
    List<ParticipeDetalleGeneracionArchivo> selectByEntidad(Long codigoEntidad) throws Throwable;
    
    /**
     * Busca todos los registros de una generación completa.
     * 
     * @param codigoGeneracion Código de la generación (GNAP)
     * @return Lista completa ordenada por tipo de producto y número de línea
     * @throws Throwable Si ocurre un error
     */
    List<ParticipeDetalleGeneracionArchivo> selectByGeneracion(Long codigoGeneracion) throws Throwable;
}
