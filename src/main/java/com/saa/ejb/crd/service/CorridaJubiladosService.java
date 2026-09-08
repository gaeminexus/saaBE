package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.CorridaJubilados;

import jakarta.ejb.Local;

/**
 * Cabecera de seguimiento de los dos procesos mensuales de jubilados (seguro médico al inicio
 * de mes, pensiones al final). Ver
 * docs/logica-negocio/crd/API-DOS-PROCESOS-MENSUALES-JUBILADOS.md.
 *
 * @author Sistema SAA
 * @since 2026-09-07
 */
@Local
public interface CorridaJubiladosService extends EntityService<CorridaJubilados> {

    /**
     * La corrida de un período, o {@code null} si nunca se generó nada — es la consulta que
     * usan las guardas de idempotencia de los dos procesos y el endpoint de seguimiento.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @param anio       : Año del período
     * @param mes        : Mes del período, 1 a 12
     * @return           : La corrida del período, o {@code null}
     * @throws Throwable : Excepcion
     */
    CorridaJubilados selectByPeriodo(Long idEmpresa, Long anio, Long mes) throws Throwable;
}
