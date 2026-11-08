package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.EstadoAprobacionPrestamo;
import com.saa.ejb.crd.service.dto.ReglaAprobacionCompleta;
import com.saa.ejb.crd.service.dto.ResultadoNivelAprobacion;
import com.saa.ejb.crd.service.dto.SimulacionReglaAprobacion;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * Aprobación del préstamo por NIVELES configurables (DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B4): la
 * regla (CRD.RGAP) se elige por tipo de préstamo, monto y plazo; sus niveles (CRD.NVAP) se aprueban en orden; en cada
 * nivel aprueba cualquiera de sus usuarios (CRD.UNAP); cada aprobación queda en la bitácora CRD.APPR.
 *
 * <p>Transición: mientras NO exista ninguna regla activa en toda la tabla, el préstamo se aprueba como hoy, en un paso.
 * En cuanto exista una, un préstamo sin regla aplicable no se aprueba.</p>
 */
@Local
public interface NivelAprobacionService {

    /** La regla con sus niveles y usuarios activos (pantalla de configuración). */
    ReglaAprobacionCompleta reglaCompleta(Long idRegla) throws Throwable;

    /** Qué regla aplicaría a un tipo, monto y plazo (sin préstamo). Nunca lanza por la configuración: la explica. */
    SimulacionReglaAprobacion simular(Long idTipoPrestamo, Double monto, Long plazo) throws Throwable;

    /**
     * Estado de la aprobación de un préstamo: la regla, cada nivel con quién y cuándo aprobó, el nivel pendiente y si el
     * usuario consultado ({@code idUsuario}, SCP.PJRQ) puede aprobarlo hoy. Nunca lanza por la configuración.
     */
    EstadoAprobacionPrestamo estadoAprobacion(Long idPrestamo, Long idUsuario) throws Throwable;

    /**
     * Registra la aprobación del nivel pendiente por {@code usuario} (ya validado). Sin niveles configurados devuelve
     * {@code conNiveles = false} y no registra nada: el llamador aprueba como hoy. Se llama DENTRO de la transacción del
     * aprobar: si después falla, la aprobación se revierte con ella.
     *
     * @throws com.saa.basico.util.IncomeException con el motivo: usuario que no es del nivel, mismo usuario en dos niveles,
     *         configuración ambigua o incompleta, sin regla aplicable
     */
    ResultadoNivelAprobacion registrarAprobacion(Prestamo prestamo, Long idUsuario, String usuario, String observacion)
            throws Throwable;

    /**
     * Rechazo con niveles: si hay regla, el usuario tiene que ser de algún nivel de ella y se registra la negación del
     * nivel pendiente. Sin niveles configurados, o sin regla aplicable, no hace nada (rechazo como hoy).
     *
     * @throws com.saa.basico.util.IncomeException si hay regla y el usuario no es de ninguno de sus niveles
     */
    void registrarRechazo(Prestamo prestamo, Long idUsuario, String usuario, String observacion) throws Throwable;

    /**
     * Pasa a {@code APPRESTD = 0} las aprobaciones vigentes del préstamo (el préstamo cambió: la aprobación vuelve a
     * empezar). Idempotente.
     *
     * @param motivo qué lo invalidó (queda en {@code APPRUSMD})
     * @return cuántas invalidó
     */
    int invalidarAprobaciones(Long idPrestamo, String motivo) throws Throwable;
}
