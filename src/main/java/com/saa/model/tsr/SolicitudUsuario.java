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
 * <p>DTO minimo del cuerpo JSON para acciones que solo necesitan identificar
 * al usuario que las realiza (deshacer grupo, verificar cuenta/periodo). No
 * es una entidad JPA.</p>
 */
public class SolicitudUsuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
