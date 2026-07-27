/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.model.tsr;

import java.io.Serializable;

/**
 * @author GaemiSoft
 * <p>DTO de una fila del drill-down por cuenta bancaria del tablero de
 * cumplimiento (¿esta cuenta ya cargó su extracto este período? ¿ya está
 * conciliada?). No es una entidad JPA - se usa solo para la respuesta del
 * endpoint /cteb/detalleCuentas.</p>
 */
public class DetalleCumplimientoCuenta implements Serializable {

    private static final long serialVersionUID = 1L;

    private CuentaBancaria cuentaBancaria;
    private boolean cargada;
    private boolean conciliada;

    public CuentaBancaria getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public boolean isCargada() {
        return cargada;
    }

    public void setCargada(boolean cargada) {
        this.cargada = cargada;
    }

    public boolean isConciliada() {
        return conciliada;
    }

    public void setConciliada(boolean conciliada) {
        this.conciliada = conciliada;
    }
}
