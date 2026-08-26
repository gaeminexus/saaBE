package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.TipoAdjunto;

import jakarta.ejb.Local;

@Local
public interface TipoAdjuntoDaoService extends EntityDao<TipoAdjunto> {

    /**
     * Busca en el catálogo por nombre exacto (sin distinguir mayúsculas/minúsculas), entre los
     * tipos ACTIVOS. Se usa para resolver "CERTIFICADO BANCARIO" sin depender de un código fijo
     * que solo se conoce después de correr el script de carga.
     *
     * @param nombre Nombre del tipo de adjunto
     * @return Lista de coincidencias (normalmente 0 o 1; si hay más de una, es un problema de
     *         datos del catálogo, no de esta consulta)
     * @throws Throwable Si ocurre un error
     */
    List<TipoAdjunto> selectByNombre(String nombre) throws Throwable;
}
