package com.saa.ejb.crd.service;

import java.util.Map;

import com.saa.ejb.crd.service.dto.SolicitudEnsayoSaldoOtros;

import jakarta.ejb.Local;

/**
 * C35 — Ensayo SIN EFECTO de la regla de {@code DTPRSLOT} sobre un préstamo real: corre los pasos pedidos
 * (reapertura al corte, anulación, precancelación, abono a capital, recálculo desde pagos) con el código real, en
 * UNA transacción que al final se revierte siempre ({@code setRollbackOnly}). Devuelve la foto de la tabla antes y
 * después de cada paso. Sirve para probar la corrección sobre casos reales sin dejar asientos, números de asiento ni
 * cambios en la base de trabajo (una prueba que genera asientos en un mes real ocupa números de la serie).
 */
@Local
public interface EnsayoSaldoOtrosService {

    /**
     * @param solicitud Préstamo, empresa, usuario y pasos
     * @return Mapa con {@code inicial}, {@code pasos} (cada uno con su foto o su error) y {@code revertido = true}
     * @throws Throwable Si ocurre un error fuera de los pasos
     */
    Map<String, Object> ensayar(SolicitudEnsayoSaldoOtros solicitud) throws Throwable;
}
