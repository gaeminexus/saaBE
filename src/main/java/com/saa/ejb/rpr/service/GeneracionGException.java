package com.saa.ejb.rpr.service;

import jakarta.ejb.ApplicationException;

/**
 * Excepción de aplicación para fallos en la generación de reportes G.
 * Al ser @ApplicationException(rollback=false), un fallo en un G NO contamina
 * la transacción del orquestador, permitiendo que los demás Gs continúen.
 */
@ApplicationException(rollback = false)
public class GeneracionGException extends Exception {

    private static final long serialVersionUID = 1L;

    public GeneracionGException(String message) {
        super(message);
    }

    public GeneracionGException(String message, Throwable cause) {
        super(message, cause);
    }
}
