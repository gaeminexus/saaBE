package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.crd.service.dto.ContextoSustitucion;
import com.saa.ejb.crd.service.dto.SustituiblePrestamo;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * Reestructuración y novación de créditos (DISENO-REESTRUCTURACION-NOVACION.md, RN1–RN14).
 * Un préstamo NUEVO (de un producto RESTRUCTURADO o NOVACION) sustituye a un original: el valor de
 * sustitución (VS) sale de {@code ProcesoPagoPrestamoService.calcularDesgloseConceptos}, el mismo
 * servicio de la precancelación, sin fórmula nueva.
 */
@Local
public interface SustitucionPrestamoService {

    String TIPO_REESTRUCTURACION = "REESTRUCTURACION";
    String TIPO_NOVACION = "NOVACION";

    /** Días máximos de atraso para reestructurar (RN4). */
    int MAX_DIAS_VENCIDO_REESTRUCTURAR = 60;

    /**
     * Préstamos del partícipe que se pueden reestructurar o novar a hoy, con su VS. Los que no se
     * pueden usar vienen con {@code puede = false} y el {@code motivo}.
     *
     * @param tipo REESTRUCTURACION o NOVACION
     */
    List<SustituiblePrestamo> sustituibles(Long idEntidad, String tipo) throws Throwable;

    /**
     * Al guardar un préstamo con {@code idPrestamoOriginal}: el original es del mismo partícipe,
     * está vivo y cumple RN4, RN9 y RN10; reestructuración: monto = VS ± 0,01; novación: monto &gt; VS.
     *
     * @throws com.saa.basico.util.IncomeException con el motivo
     */
    void validarAlGuardar(Prestamo nuevo) throws Throwable;

    /**
     * Al aprobar: recalcula el VS, revalida y aplica RN7 y RN8.
     *
     * @throws com.saa.basico.util.IncomeException con el motivo
     */
    ContextoSustitucion prepararAprobacion(Prestamo nuevo) throws Throwable;

    /**
     * Cancela el original (RN11): cuotas pendientes a CANCELADA_ANTICIPADA (7), un EventoPrestamo
     * REESTRUCTURACION o NOVACION con el VS, y el estado 5 o 13. Escribe en el NUEVO el VS, el
     * enlace y las banderas (RN12). Sin PagoPrestamo: no entra dinero.
     *
     * @param idAsiento código del asiento del traslado (puede ser null si la contabilidad está inactiva)
     */
    void cancelarOriginal(Prestamo nuevo, ContextoSustitucion contexto, Long idAsiento, String usuario)
            throws Throwable;
}
