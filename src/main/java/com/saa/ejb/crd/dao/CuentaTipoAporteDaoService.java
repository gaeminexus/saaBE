package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CuentaTipoAporte;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA
 *         Interface DAO para la entidad CuentaTipoAporte (CRD.CTAP).
 */
@Local
public interface CuentaTipoAporteDaoService extends EntityDao<CuentaTipoAporte> {

    /**
     * La configuración de un tipo de aporte para una empresa — a lo sumo una, por
     * {@code UK_CTAP_TPAP_PJRQ}. Es la consulta que usa el asiento de reclasificación de la
     * devolución de aportes.
     *
     * <b>⚠️ Filtra por {@code estado = ACTIVO} a propósito — no es decorativo, es lo que hace
     * que la baja lógica funcione de verdad.</b> No hay pantalla de mantenimiento todavía
     * (2026-08-31: aprobada, en cola), así que la única forma de dar de baja una fila mal
     * cargada es poner {@code CTAPESTD = 0} directo en la base. Si algún día alguien saca este
     * filtro (p.ej. al generalizar la consulta), cada baja lógica vuelve a resolver su cuenta
     * como si estuviera activa — el asiento de reclasificación saldría contra una cuenta que
     * alguien creyó haber dado de baja, y cuadraría igual. Por el mismo motivo la
     * implementación usa {@code getResultList()} y no {@code getSingleResult()}: "no hay
     * configuración" es una ausencia de dato normal (tipo sin mapear), no una excepción de
     * infraestructura.
     *
     * @param idTipoAporte : Código del tipo de aporte (CRD.TPAP)
     * @param idEmpresa    : Código de la empresa (SCP.PJRQ)
     * @return             : La configuración activa, o {@code null} si el tipo no está mapeado
     *                       (o fue dado de baja con {@code CTAPESTD = 0})
     * @throws Throwable   : Excepcion
     */
    CuentaTipoAporte selectByTipoAporteYEmpresa(Long idTipoAporte, Long idEmpresa) throws Throwable;

    /**
     * Todas las configuraciones ACTIVAS de una empresa, ordenadas por tipo de aporte. Para el
     * mantenimiento y para el diagnóstico de qué tipos quedaron sin mapear. Mismo filtro por
     * {@code estado} que {@link #selectByTipoAporteYEmpresa} y por la misma razón: no es
     * decorativo.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @return           : Listado; VACÍO si la empresa no tiene ninguna configuración activa
     * @throws Throwable : Excepcion
     */
    List<CuentaTipoAporte> selectByEmpresa(Long idEmpresa) throws Throwable;
}
