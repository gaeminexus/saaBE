package com.saa.ejb.rhh.util;

import com.saa.basico.util.IncomeException;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.rubros.RhhEstadoPeriodoNomina;

/**
 * Guarda comun para decidir si un periodo de nomina admite todavia un cambio "menor"
 * -una novedad, un valor no pagado, una solicitud de vacaciones aprobada- que no pasa por
 * el flujo normal de calculo/aprobacion/contabilizacion.
 *
 * <p>Centraliza lo que hasta el 2026-09-08 eran tres copias identicas
 * ({@code ValorNoPagadoServiceImpl}, {@code OrdenBeneficioSocialServiceImpl},
 * {@code SolicitudVacacionesServiceImpl} al agregarse esta ultima), todas con el mismo
 * criterio y el mismo texto de mensaje. Se copio dos veces por apuro de bloqueos urgentes de
 * produccion sin volver atras a extraer esto; al aparecer una tercera necesidad se junta
 * aqui, siguiendo la misma convencion de utilitario estatico que {@link RedondeoNomina}.</p>
 *
 * <p><b>Por que ABIERTO o CALCULADO, y no ABIERTO estricto:</b> {@code reabrirPeriodo} deja
 * un periodo en CALCULADO(3), nunca en ABIERTO(1) -no hay ningun camino en todo el proyecto
 * que devuelva un periodo a ABIERTO desde CALCULADO-, asi que exigir ABIERTO estricto es una
 * pared sin puerta para cualquier cosa que necesite registrarse despues de una reapertura.
 * Estos cambios son ademas informativos/aditivos: no tocan bases de IESS/IR ni contabilidad
 * por si solos, asi que registrarlos en un periodo ya calculado no corrompe nada -solo no se
 * ve hasta que alguien recalcule, por eso quien llama a {@link #exige} normalmente avisa al
 * usuario que falta ese recalculo.</p>
 *
 * <ul>
 * <li>{@code ABIERTO}(1) o {@code CALCULADO}(3): permitido.</li>
 * <li>{@code EN_CALCULO}(2): rechazado -- registrar en medio de un calculo es una carrera
 * contra el motor que esta leyendo/escribiendo esta misma nomina.</li>
 * <li>Cualquier otro ({@code >= APROBADO}): rechazado, ya no admite cambios de nomina.</li>
 * </ul>
 *
 * @author GaemiSoft
 */
public final class PeriodoModificableNomina {

    private PeriodoModificableNomina() {
    }

    /**
     * Exige que el periodo admita todavia un cambio menor de nomina.
     *
     * @param periodo		: Periodo de nomina
     * @param operacion		: Texto de la operacion, para el mensaje ("registrar un valor no
     *						  pagado", "aprobar la solicitud de vacaciones", ...)
     * @throws Throwable	: IncomeException si el periodo no admite la operacion
     */
    public static void exige(PeriodoNomina periodo, String operacion) throws Throwable {
        int estado = periodo.getEstado() != null ? periodo.getEstado().intValue() : -1;
        if (estado == RhhEstadoPeriodoNomina.ABIERTO || estado == RhhEstadoPeriodoNomina.CALCULADO) {
            return;
        }
        if (estado == RhhEstadoPeriodoNomina.EN_CALCULO) {
            throw new IncomeException("El rol del periodo " + periodo.getMes() + "/" + periodo.getAnio()
                    + " se esta calculando: espere a que termine antes de " + operacion + ".");
        }
        throw new IncomeException("El periodo " + periodo.getMes() + "/" + periodo.getAnio() + " esta "
                + textoEstado(periodo.getEstado()) + ": ya no admite cambios de nomina.");
    }

    /**
     * Nombre legible de un estado de {@code RhhEstadoPeriodoNomina}, para mensajes al usuario.
     *
     * @param estado	: Codigo de RHH.PRDN.PRDNESTD; admite nulo
     * @return			: Nombre del estado, o una descripcion generica si no se reconoce
     */
    public static String textoEstado(Long estado) {
        if (estado == null) {
            return "en un estado desconocido";
        }
        switch (estado.intValue()) {
            case RhhEstadoPeriodoNomina.ABIERTO:
                return "ABIERTO";
            case RhhEstadoPeriodoNomina.EN_CALCULO:
                return "EN_CALCULO";
            case RhhEstadoPeriodoNomina.CALCULADO:
                return "CALCULADO";
            case RhhEstadoPeriodoNomina.APROBADO:
                return "APROBADO";
            case RhhEstadoPeriodoNomina.CONTABILIZADO:
                return "CONTABILIZADO";
            case RhhEstadoPeriodoNomina.PAGADO:
                return "PAGADO";
            case RhhEstadoPeriodoNomina.CERRADO:
                return "CERRADO";
            case RhhEstadoPeriodoNomina.ANULADO:
                return "ANULADO";
            default:
                return "en estado " + estado;
        }
    }
}
