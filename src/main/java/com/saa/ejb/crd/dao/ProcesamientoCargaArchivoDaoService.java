package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ProcesamientoCargaArchivo;

import jakarta.ejb.Local;

@Local
public interface ProcesamientoCargaArchivoDaoService extends EntityDao<ProcesamientoCargaArchivo> {
    
    /**
     * Busca el procesamiento de un registro específico de ParticipeXCargaArchivo
     * @param idParticipeXCarga ID del ParticipeXCargaArchivo
     * @return ProcesamientoCargaArchivo si existe, null si no
     * @throws Throwable Si ocurre un error
     */
    ProcesamientoCargaArchivo selectByParticipeXCarga(Long idParticipeXCarga) throws Throwable;
    
    /**
     * Verifica si un registro ya fue procesado
     * @param idParticipeXCarga ID del ParticipeXCargaArchivo
     * @return true si ya fue procesado, false si no
     * @throws Throwable Si ocurre un error
     */
    boolean yaFueProcesado(Long idParticipeXCarga) throws Throwable;

}
