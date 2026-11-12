package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.model.rpr.CreditoCuotasPrestamosBanda;

import jakarta.ejb.Local;

/**
 * C27-D — acceso a RPR.CCPB (detalle del CCPM por rubro y banda). Ver
 * {@link CreditoCuotasPrestamosBanda}.
 */
@Local
public interface CreditoCuotasPrestamosBandaDaoService {

    /** Inserta un detalle. */
    void insertar(CreditoCuotasPrestamosBanda detalle) throws Throwable;

    /** Borra el detalle de una ejecución (antes de borrar el CCPM y la ejecución). */
    int deleteByEjecucion(Long codigoEjecucion) throws Throwable;

    /** Detalle completo de una ejecución, por operación. */
    List<CreditoCuotasPrestamosBanda> selectByEjecucion(Long codigoEjecucion) throws Throwable;

    /**
     * Totales de la ejecución por rubro, tipo de préstamo, banda y cuenta:
     * {rubro, idTipoPrestamo, numeroBanda, etiquetaBanda, cuentaContable, valor (redondeado a 2), operaciones}.
     */
    List<Object[]> selectTotalesPorRubro(Long codigoEjecucion) throws Throwable;
}
