package com.saa.ejb.rpr.service;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C50: regla pura (sin EJB ni BD) de si una ejecución de las G40-G51 sigue valiendo.
 * <ul>
 * <li>{@link #SIN_DATO}: la ejecución no tiene corrida de cartera grabada (generada antes del C50). Ni vigente ni desactualizada: el histórico no se toca.</li>
 * <li>{@link #DESACTUALIZADA} (a): la corrida de cartera con la que se generó ya no es la vigente del mes (reversada o reemplazada).</li>
 * <li>{@link #DESACTUALIZADA} (b): hay un asiento vigente del mes en 1.3, 1.4 o 2.1 registrado después del último que existía al generar.</li>
 * <li>{@link #VIGENTE}: ninguna de las dos.</li>
 * </ul>
 * Contrato: docs/logica-negocio/rpr/C50-G-ATADAS-A-LA-CORRIDA.md.
 */
public final class ReglaActualizacionG {

    public static final String SIN_DATO = "SIN_DATO";
    public static final String VIGENTE = "VIGENTE";
    public static final String DESACTUALIZADA = "DESACTUALIZADA";

    private ReglaActualizacionG() {
    }

    /** Resultado de aplicar la regla. */
    public static class Resultado implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String estado;
        private final String motivo;

        Resultado(String estado, String motivo) {
            this.estado = estado;
            this.motivo = motivo;
        }

        public String getEstado() { return estado; }
        public String getMotivo() { return motivo; }
        public boolean estaDesactualizada() { return DESACTUALIZADA.equals(estado); }
    }

    /** Agrega al motivo de una ejecucion desactualizada por asiento posterior el numero y la fecha contable de ese asiento. */
    public static Resultado conAsiento(Resultado base, Object numero, Object fechaContable) {
        return new Resultado(base.getEstado(), base.getMotivo() + " Asiento " + numero + " con fecha contable " + fechaContable + ".");
    }

    /**
     * @param corridaGrabada   corrida de cartera con la que se generó la ejecución (EJRCCRCT); nula = sin dato
     * @param corridaVigente   corrida de cartera vigente HOY del mes; nula = el mes no tiene corrida vigente
     * @param ultimoGrabado    registro del último asiento vigente del mes en 1.3/1.4/2.1 al generar (EJRCFCUA)
     * @param generadaEn       instante de generación (EJRCFCHG); solo se usa si no hay {@code ultimoGrabado}
     * @param ultimoAhora      registro del último asiento vigente del mes en 1.3/1.4/2.1 HOY; nulo = ninguno
     */
    public static Resultado evaluar(Long corridaGrabada, Long corridaVigente, LocalDateTime ultimoGrabado,
            LocalDateTime generadaEn, LocalDateTime ultimoAhora) {
        if (corridaGrabada == null) {
            return new Resultado(SIN_DATO, "La ejecucion no tiene corrida de cartera grabada (generada antes del C50).");
        }
        if (corridaVigente == null) {
            return new Resultado(DESACTUALIZADA, "Se genero con la corrida de cartera " + corridaGrabada
                    + " y el mes ya no tiene una corrida de cartera vigente (reversada).");
        }
        if (corridaVigente.longValue() != corridaGrabada.longValue()) {
            return new Resultado(DESACTUALIZADA, "Se genero con la corrida de cartera " + corridaGrabada
                    + " y la vigente del mes es la " + corridaVigente + " (la anterior se reverso o se reemplazo).");
        }
        LocalDateTime referencia = ultimoGrabado != null ? ultimoGrabado : generadaEn;
        if (ultimoAhora != null && referencia != null && ultimoAhora.isAfter(referencia)) {
            return new Resultado(DESACTUALIZADA, "Hay un asiento vigente del mes en cartera o aportes registrado el " + ultimoAhora
                    + ", despues de la generacion (ultimo asiento conocido al generar: " + referencia + ").");
        }
        return new Resultado(VIGENTE, "La ejecucion corresponde a la corrida de cartera vigente " + corridaVigente + ".");
    }
}
