package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Producto;

import jakarta.ejb.Local;

@Local
public interface ProductoDaoService extends EntityDao<Producto> {

    /**
     * Busca un producto por su código Petro (retorna el primero si hay múltiples)
     * @param codigoPetro Código del producto según PETROCOMERCIAL
     * @return Producto encontrado o null
     * @throws Throwable Si ocurre un error
     */
    Producto selectByCodigoPetro(String codigoPetro) throws Throwable;

    /**
     * Busca TODOS los productos con el mismo código Petro
     * Un código Petro puede mapear a múltiples productos en nuestra base
     * @param codigoPetro Código del producto según PETROCOMERCIAL
     * @return Lista de productos encontrados
     * @throws Throwable Si ocurre un error
     */
    List<Producto> selectAllByCodigoPetro(String codigoPetro) throws Throwable;

}
