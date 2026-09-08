package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CorridaJubilados;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad CorridaJubilados (CRD.CRJB): cabecera de seguimiento
 *         de los dos procesos mensuales de jubilados (seguro médico, pensiones).
 */
@Local
public interface CorridaJubiladosDaoService extends EntityDao<CorridaJubilados> {

    /**
     * La corrida de un período (empresa + año + mes), si existe. Es la consulta de
     * idempotencia de los dos procesos: si devuelve algo, hay que mirar sus dos estados
     * antes de generar cualquier cosa.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @param anio       : Año del período
     * @param mes        : Mes del período, 1 a 12
     * @return           : La corrida del período, o {@code null} si nunca se generó nada
     * @throws Throwable : Excepcion
     */
    CorridaJubilados selectByPeriodo(Long idEmpresa, Long anio, Long mes) throws Throwable;
}
