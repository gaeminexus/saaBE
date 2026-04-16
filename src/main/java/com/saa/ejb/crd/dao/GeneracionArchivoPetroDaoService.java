package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.GeneracionArchivoPetro;

import jakarta.ejb.Local;

/**
 * Interface DAO para GeneracionArchivoPetro (GNAP).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Local
public interface GeneracionArchivoPetroDaoService extends EntityDao<GeneracionArchivoPetro> {

    /**
     * Busca una generación por periodo (mes/año) y filial.
     * 
     * @param mes Mes del periodo (1-12)
     * @param anio Año del periodo
     * @param codigoFilial Código de la filial
     * @return Generación encontrada o null
     * @throws Throwable Si ocurre un error
     */
    GeneracionArchivoPetro selectByPeriodo(Integer mes, Integer anio, Long codigoFilial) throws Throwable;
    
    /**
     * Lista todas las generaciones de una filial ordenadas por fecha desc.
     * 
     * @param codigoFilial Código de la filial
     * @return Lista de generaciones
     * @throws Throwable Si ocurre un error
     */
    List<GeneracionArchivoPetro> selectByFilial(Long codigoFilial) throws Throwable;
    
    /**
     * Lista generaciones por estado.
     * 
     * @param estado Estado (1=GENERADO, 2=ENVIADO, 3=PROCESADO)
     * @return Lista de generaciones
     * @throws Throwable Si ocurre un error
     */
    List<GeneracionArchivoPetro> selectByEstado(Integer estado) throws Throwable;
}
