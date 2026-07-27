/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.rubros;

/**
 * @author GaemiSoft
 * <p>Estado de revisión de un {@link com.saa.model.tsr.ConciliacionContable}
 * (conciliación de extracto bancario contra contabilidad, por cuenta
 * bancaria y período). No confundir con el estado de registro
 * (activo/inactivo, ver {@link Estado}) - este es el estado del proceso de
 * conciliación en sí, no del registro.</p>
 */
public interface EstadoConciliacionContable {

    public static final int PENDIENTE = 1;
    public static final int VERIFICADO = 2;
    public static final int CON_DIFERENCIAS = 3;

}
