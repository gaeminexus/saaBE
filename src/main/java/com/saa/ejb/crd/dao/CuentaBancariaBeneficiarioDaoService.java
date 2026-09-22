package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CuentaBancariaBeneficiario;

import jakarta.ejb.Local;

@Local
public interface CuentaBancariaBeneficiarioDaoService extends EntityDao<CuentaBancariaBeneficiario> {

    /**
     * Beneficiarios de un partícipe, activos e inactivos, ordenados por porcentaje descendente y
     * luego por código. Es la consulta que alimenta la pantalla (§3.1 del contrato).
     *
     * @param idEntidad código del partícipe (CRD.ENTD)
     * @return lista de beneficiarios (puede ser vacía)
     * @throws Throwable Si ocurre un error
     */
    List<CuentaBancariaBeneficiario> selectPorEntidad(Long idEntidad) throws Throwable;

    /**
     * Beneficiario de un partícipe por su identificación, para el chequeo de duplicado antes de
     * insertar (§3.3 del contrato: el índice único es (ENTDCDGO, CBBPIDNT), no por identificación
     * sola — la misma cédula puede ser beneficiaria de varios partícipes).
     *
     * @param idEntidad código del partícipe (CRD.ENTD)
     * @param identificacion identificación del beneficiario
     * @return lista de coincidencias (normalmente 0 o 1; si hay más de una es un problema de datos)
     * @throws Throwable Si ocurre un error
     */
    List<CuentaBancariaBeneficiario> selectPorEntidadEIdentificacion(Long idEntidad, String identificacion) throws Throwable;
}
