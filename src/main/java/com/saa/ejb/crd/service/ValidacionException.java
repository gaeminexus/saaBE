package com.saa.ejb.crd.service;

import com.saa.basico.util.IncomeException;

/**
 * Excepción de validación de entrada que el REST debe responder con 400 Bad Request, NO
 * con el 401 que usa {@link UsuarioAppService} para fallas de credenciales. Se distingue
 * por TIPO, no por el texto del mensaje — mismo criterio que
 * {@code com.saa.ejb.cxp.service.ConflictoNegocioException}: matchear por el texto del
 * mensaje se rompe en silencio el día que alguien lo reescribe.
 * <p>
 * Uso actual: {@code UsuarioAppServiceImpl#cambiarClave} — la clave nueva que no cumple la
 * política (8 caracteres, letras y números) NO es una falla de "quién sos" (401), es una
 * falla de "lo que mandaste no sirve" (400). La distinción importa en la práctica: la app
 * móvil trata cualquier 401 como sesión vencida y fuerza el logout — si la clave nueva
 * floja devolviera 401, un partícipe recién enrolado (con {@code debeCambiarClave} en
 * true) quedaría en un loop de login sin poder activar nunca su cuenta.
 */
public class ValidacionException extends IncomeException {

    private static final long serialVersionUID = 1L;

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
