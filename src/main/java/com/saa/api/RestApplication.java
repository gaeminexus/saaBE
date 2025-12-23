package com.saa.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Configuración base JAX-RS para SaaBE
 * 
 * - Usa JSON-B (proveído por WildFly)
 * - NO registra Jackson
 * - Compatible con Jakarta EE 10
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
    // No se sobreescribe nada a propósito.
    // WildFly autodetecta los resources (@Path)
    // y usa JSON-B por defecto.
}