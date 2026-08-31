package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.CuentaTipoAporte;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA
 *         Cuentas contables por tipo de aporte y empresa (CRD.CTAP) — fuente única de las
 *         cuentas del asiento de reclasificación de la devolución de aportes. Ver el javadoc
 *         de {@link CuentaTipoAporte}.
 */
@Local
public interface CuentaTipoAporteService extends EntityService<CuentaTipoAporte> {

    /**
     * La configuración de un tipo de aporte para una empresa, o {@code null} si el tipo no
     * está mapeado. Simple passthrough del DAO — no hay lógica de vigencia que resolver.
     *
     * @param idTipoAporte : Código del tipo de aporte (CRD.TPAP)
     * @param idEmpresa    : Código de la empresa (SCP.PJRQ)
     * @throws Throwable   : Excepcion
     */
    CuentaTipoAporte selectByTipoAporteYEmpresa(Long idTipoAporte, Long idEmpresa) throws Throwable;

    /**
     * Listado completo de una empresa, para el mantenimiento. Lista VACÍA no es error: una
     * empresa nueva simplemente no tiene nada configurado todavía.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @throws Throwable : Excepcion
     */
    List<CuentaTipoAporte> listarPorEmpresa(Long idEmpresa) throws Throwable;
}
