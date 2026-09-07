package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.SolicitudCambiarClaveUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudCrearUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudDesactivarUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudReactivarUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudResetearClaveUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudValidarCredencial;
import com.saa.ejb.crd.service.dto.ValidarCredencialResponse;
import com.saa.model.crd.UsuarioApp;

import jakarta.ejb.Local;

/**
 * Credenciales de acceso a la app móvil ASOPREP (CRD.USAP). El WAR de borde (SaaMovilBE,
 * expuesto a internet) es el único cliente de {@link #validarCredencial}; SaaBE no se
 * expone directamente a la app.
 *
 * @author Sistema SAA
 * @since 2026-09-03
 */
@Local
public interface UsuarioAppService {

    /**
     * Valida identificación + clave para el login de la app móvil. Nunca revela si el
     * usuario existe: identificación inexistente, clave incorrecta, y usuario
     * bloqueado/eliminado devuelven exactamente el mismo error genérico.
     *
     * En éxito: resetea {@code intentosFallidos} a 0 y graba {@code fechaUltimoAcceso}.
     * En fallo: incrementa {@code intentosFallidos}; al llegar a 5 bloquea la cuenta por
     * 15 minutos ({@code bloqueadoHasta = ahora + 15min}, estado BLOQUEADO).
     *
     * @param solicitud  : identificación + clave en texto plano
     * @return           : Datos del partícipe para la sesión de la app
     * @throws Throwable : {@link com.saa.basico.util.IncomeException} con mensaje genérico
     *                     "credenciales invalidas" si la validación falla
     */
    ValidarCredencialResponse validarCredencial(SolicitudValidarCredencial solicitud) throws Throwable;

    /**
     * Cambia la clave, validando la actual. La nueva clave exige mínimo 8 caracteres con
     * letras y números. Apaga el flag {@code debeCambiarClave}.
     *
     * @param solicitud  : identificación + clave actual + clave nueva
     * @throws Throwable : Si la identificación no existe, la clave actual no valida, o la
     *                     nueva no cumple la política
     */
    void cambiarClave(SolicitudCambiarClaveUsuarioApp solicitud) throws Throwable;

    /**
     * Enrola a un partícipe en la app móvil (lo ejecuta la oficina desde el FE de
     * intranet). Busca el partícipe en CRD.ENTD por identificación; si no existe, error
     * claro. Crea la credencial con el flag {@code debeCambiarClave} encendido.
     *
     * @param solicitud  : identificación (debe existir en CRD.ENTD) + clave temporal +
     *                     {@code usuario} (obligatorio — quien enrola desde la oficina;
     *                     lo manda el FE de intranet, queda en CRD.USAP.USAPUSAR)
     * @return           : La credencial creada
     * @throws Throwable : Si el partícipe no existe, o ya tiene una credencial
     */
    UsuarioApp crear(SolicitudCrearUsuarioApp solicitud) throws Throwable;

    /**
     * La oficina resetea la clave de un partícipe: nueva clave temporal, enciende
     * {@code debeCambiarClave} y desbloquea la cuenta (limpia intentos fallidos y
     * {@code bloqueadoHasta}).
     *
     * @param solicitud  : identificación + clave temporal nueva + {@code usuario}
     *                     (obligatorio — quien resetea desde la oficina; lo manda el FE de
     *                     intranet, SIEMPRE sobreescribe CRD.USAP.USAPUSAR, incluso si ya
     *                     tenía el usuario de un enrolamiento anterior)
     * @return           : La credencial actualizada
     * @throws Throwable : Si la identificación no tiene credencial, o si está en estado
     *                     ELIMINADO — ahí no aplica: revertir un borrado es un acto
     *                     deliberado y separado, ver {@link #reactivar}
     */
    UsuarioApp resetearClave(SolicitudResetearClaveUsuarioApp solicitud) throws Throwable;

    /**
     * Revierte el borrado (ELIMINADO) de una cuenta — a propósito un endpoint separado de
     * {@link #resetearClave}, para que reactivar una cuenta eliminada sea un acto
     * deliberado del empleado de oficina y quede auditado, nunca un efecto lateral
     * invisible de un reseteo de rutina. Solo aplica si la cuenta está ELIMINADO; si está
     * ACTIVO o BLOQUEADO se rechaza (ese caso es {@link #resetearClave}).
     *
     * Endpoint de INTRANET únicamente — SaaMovilBE nunca lo consume ni lo conoce.
     *
     * @param solicitud  : identificación + clave temporal nueva + {@code usuario}
     *                     (obligatorio — quien reactiva desde la oficina, queda en
     *                     CRD.USAP.USAPUSAR)
     * @return           : La credencial reactivada, en estado ACTIVO con
     *                     {@code debeCambiarClave} encendido
     * @throws Throwable : Si la identificación no tiene credencial, o si no está en
     *                     estado ELIMINADO
     */
    UsuarioApp reactivar(SolicitudReactivarUsuarioApp solicitud) throws Throwable;

    /**
     * "Eliminación de cuenta" exigida por Google Play y App Store: valida credenciales y
     * pone la cuenta en estado ELIMINADO. Elimina el ACCESO desde la app — los datos del
     * partícipe en el fondo (CRD.ENTD y todo lo demás) no se tocan, se rigen por la
     * relación contractual.
     *
     * @param solicitud  : identificación + clave (se exige para no permitir que cualquiera
     *                     desactive la cuenta de otro)
     * @throws Throwable : Mismo error genérico que {@link #validarCredencial} si las
     *                     credenciales no validan
     */
    void desactivar(SolicitudDesactivarUsuarioApp solicitud) throws Throwable;
}
