package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

@Local
public interface PrestamoDaoService extends EntityDao<Prestamo> {
    
    /**
     * Busca un préstamo por su idAsoprep.
     * @param idAsoprep ID del asociado préstamo
     * @return Préstamo encontrado o null si no existe
     * @throws Throwable Si ocurre algún error
     */
    Prestamo selectByIdAsoprep(Long idAsoprep) throws Throwable;
    
    /**
     * Busca préstamos activos de una entidad para un producto específico.
     * Usado en FASE 2 para encontrar el préstamo correspondiente a un registro del archivo.
     * @param rolPetroComercial Código Petro de la entidad (String)
     * @param codigoPetroProducto Código Petro del producto
     * @return Lista de préstamos activos que coinciden
     * @throws Throwable Si ocurre algún error
     */
    List<Prestamo> selectByEntidadYProductoActivos(String rolPetroComercial, String codigoPetroProducto) throws Throwable;

    /**
     * Busca préstamos activos por IDs numéricos de entidad y producto.
     * Método más eficiente y confiable que evita problemas con espacios en códigos String.
     * @param codigoEntidad ID numérico de la entidad
     * @param codigoProducto ID numérico del producto
     * @return Lista de préstamos activos que coinciden
     * @throws Throwable Si ocurre algún error
     */
    List<Prestamo> selectByEntidadYProductoActivosById(Long codigoEntidad, Long codigoProducto) throws Throwable;

}

