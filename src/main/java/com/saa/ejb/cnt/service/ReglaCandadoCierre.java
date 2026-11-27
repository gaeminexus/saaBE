package com.saa.ejb.cnt.service;

import java.util.List;
import java.util.Locale;

import com.saa.ejb.cnt.service.dto.ExcepcionCandado;
import com.saa.ejb.cnt.service.dto.FallaCandado;

/**
 * C51: regla pura (sin EJB ni BD) de las excepciones al candado del cierre contable.
 * <ul>
 * <li>Una excepción sin motivo (vacío o solo espacios) o sin código de control es un error de la petición.</li>
 * <li>Una excepción cubre una falla cuando el código coincide (sin distinguir mayúsculas) y la falla es excepcionable. Una excepción para una condición
 * de las G (no excepcionable) se rechaza: unas G con errores o viejas no se excusan.</li>
 * <li>Una excepción para un código que no está fallando no hace nada (no se rechaza: el semáforo pudo limpiarse entre la consulta y el cierre).</li>
 * <li>Bloquea el cierre toda falla que queda sin cubrir.</li>
 * </ul>
 */
public final class ReglaCandadoCierre {

    private ReglaCandadoCierre() {
    }

    /**
     * Marca como cubiertas las fallas con excepción válida y devuelve si el cierre queda bloqueado (alguna falla sin cubrir).
     *
     * @throws IllegalArgumentException si una excepción no trae código o motivo, o si pide excusar una falla no excepcionable
     */
    public static boolean aplicar(List<FallaCandado> fallas, List<ExcepcionCandado> excepciones) {
        if (excepciones != null) {
            for (ExcepcionCandado e : excepciones) {
                if (e == null || e.getCodigoControl() == null || e.getCodigoControl().trim().isEmpty()) {
                    throw new IllegalArgumentException("Cada excepcion del candado necesita el codigo del control (codigoControl).");
                }
                if (e.getMotivo() == null || e.getMotivo().trim().isEmpty()) {
                    throw new IllegalArgumentException("La excepcion del control " + e.getCodigoControl().trim()
                            + " no tiene motivo: el motivo es obligatorio.");
                }
                for (FallaCandado f : fallas) {
                    if (f.getCodigo().toUpperCase(Locale.ROOT).equals(e.getCodigoControl().trim().toUpperCase(Locale.ROOT))) {
                        if (!f.isExcepcionable()) {
                            throw new IllegalArgumentException("El control " + f.getCodigo() + " (" + f.getDescripcion()
                                    + ") no admite excepciones: unas G con errores, incompletas o desactualizadas no se excusan.");
                        }
                        f.setCubierta(true);
                        f.setMotivoExcepcion(e.getMotivo().trim());
                    }
                }
            }
        }
        for (FallaCandado f : fallas) {
            if (!f.isCubierta()) {
                return true;
            }
        }
        return false;
    }

    /** Texto «CODIGO (descripcion) valor V; ...» de las fallas sin cubrir ({@code cubiertas = false}) o de las cubiertas ({@code true}, con su motivo). */
    public static String texto(List<FallaCandado> fallas, boolean cubiertas) {
        StringBuilder sb = new StringBuilder();
        for (FallaCandado f : fallas) {
            if (f.isCubierta() != cubiertas) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(f.getCodigo()).append(" (").append(f.getDescripcion()).append(")");
            if (f.getValor() != null) {
                sb.append(" valor ").append(String.format(Locale.US, "%.2f", f.getValor()));
            }
            if (cubiertas) {
                sb.append(" EXCEPCION: ").append(f.getMotivoExcepcion());
            }
        }
        return sb.toString();
    }
}
