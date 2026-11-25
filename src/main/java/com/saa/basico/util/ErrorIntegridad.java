package com.saa.basico.util;

import java.sql.SQLException;

/**
 * VCN #8 (dudoso): reconoce el error de Oracle «registro hijo encontrado» (ORA-02292, restricción de integridad violada) cuando se intenta borrar un registro
 * que otros usan, para responder con un mensaje claro en vez del 500 con el texto crudo de Oracle.
 */
public final class ErrorIntegridad {

    private ErrorIntegridad() {
    }

    /** {@code true} si algún eslabón de la cadena de causas es ORA-02292 (por código de error o por el texto). */
    public static boolean esRegistroConHijos(Throwable e) {
        int guarda = 0;
        for (Throwable t = e; t != null && guarda < 20; t = t.getCause(), guarda++) {
            if (t instanceof SQLException) {
                SQLException s = (SQLException) t;
                if (s.getErrorCode() == 2292) {
                    return true;
                }
            }
            String m = t.getMessage();
            if (m != null && (m.contains("ORA-02292") || m.contains("child record found"))) {
                return true;
            }
        }
        return false;
    }

    /** Mensaje para el usuario cuando el registro tiene dependientes. */
    public static String mensajeConDependientes(String objeto) {
        return "No se puede eliminar " + objeto + ": tiene registros dependientes (asientos, movimientos u otros datos que lo usan). "
            + "Desactívelo en lugar de eliminarlo.";
    }
}
