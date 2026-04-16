package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DetalleGeneracionArchivo;

import jakarta.ejb.Local;

/**
 * Interface DAO para DetalleGeneracionArchivo (DTGA).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Local
public interface DetalleGeneracionArchivoDaoService extends EntityDao<DetalleGeneracionArchivo> {

    /**
     * Busca todos los detalles de una generación específica.
     * 
     * @param codigoGeneracion Código de la generación (GNAP)
     * @return Lista de detalles por tipo de producto
     * @throws Throwable Si ocurre un error
     */
    List<DetalleGeneracionArchivo> selectByGeneracion(Long codigoGeneracion) throws Throwable;
    
    /**
     * Busca un detalle específico por generación y tipo de producto.
     * 
     * @param codigoGeneracion Código de la generación
     * @param codigoProducto Código del producto (AH, HS, PE, etc.)
     * @return Detalle encontrado o null
     * @throws Throwable Si ocurre un error
     */
    DetalleGeneracionArchivo selectByGeneracionYProducto(Long codigoGeneracion, String codigoProducto) throws Throwable;
}
