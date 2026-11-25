package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.Map;

import com.saa.ejb.crd.service.dto.ResultadoControlProvision;
import com.saa.model.crd.CorridaCierreCartera;

import jakarta.ejb.Local;

/**
 * Lote 22 (niveles 2.3, 2.4 y 3.1): control 1.4.02 = 1.4.99.05 en el cierre de cartera. Las cuentas salen de las plantillas 17 (1.4.02) y 36 (1.4.99.05),
 * nunca de un número en el código. Solo rige con el devengo completo activo al corte.
 * Contrato y reglas: docs/logica-negocio/crd/DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md (Lote 22).
 */
@Local
public interface ControlProvisionInteresService {

    /** Tolerancia por tipo de préstamo y componente. */
    double TOLERANCIA = 0.01;

    /** Origen de los MVIC del ajuste de último recurso. */
    String ORIGEN_AJUSTE_ULTIMO_RECURSO = "AJUSTE_ULTIMO_RECURSO";

    /**
     * Calcula el control al corte. Con {@code proyeccion} (previsualización) suma a cada tipo lo que el cierre todavía va a asentar: por tipo,
     * {@code [Δ contable 1.4.02, Δ libro devengo interés, Δ libro devengo mora, Δ contable 1.4.99.05, Δ libro provisión interés, Δ libro provisión mora]};
     * con null calcula lo asentado hoy.
     */
    ResultadoControlProvision calcular(Long idEmpresa, LocalDate corte, Map<Long, double[]> proyeccion) throws Throwable;

    /**
     * Después de ejecutar los pasos del cierre: calcula el control sobre lo asentado y, si hay diferencia, la registra como excepción y, con la propiedad
     * {@code saa.cartera.ajusteUltimoRecurso} encendida, genera el ajuste de último recurso (ver el doc). Va dentro de la transacción del cierre.
     */
    ResultadoControlProvision controlarYAjustar(Long idEmpresa, LocalDate corte, CorridaCierreCartera corrida, String usuario, String ip)
            throws Throwable;

    /**
     * 3.1 (informativo): después del ④ del día 1, 1.4.02 − 1.4.99.05 al {@code fechaDia1} contra el devengo del asiento del ④ ({@code idAsientoDevengo}),
     * por tipo de préstamo. Se agrega a {@code resultado}.
     */
    void controlDia1(ResultadoControlProvision resultado, Long idEmpresa, LocalDate fechaDia1, Long idAsientoDevengo) throws Throwable;

    /** El control del mes para /prdo/cerrar (2.4): lo asentado al corte, con {@code ajusteRegistrado}. */
    ResultadoControlProvision controlParaCierreContable(Long idEmpresa, LocalDate corte) throws Throwable;
}
