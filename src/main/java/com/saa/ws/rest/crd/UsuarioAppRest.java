package com.saa.ws.rest.crd;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.UsuarioAppService;
import com.saa.ejb.crd.service.ValidacionException;
import com.saa.ejb.crd.service.dto.SolicitudCambiarClaveUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudCrearUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudDesactivarUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudReactivarUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudResetearClaveUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudValidarCredencial;
import com.saa.ejb.crd.service.dto.UsuarioAppDTO;
import com.saa.ejb.crd.service.dto.ValidarCredencialResponse;
import com.saa.model.crd.UsuarioApp;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Credenciales de la app móvil ASOPREP (CRD.USAP). Consumido por el WAR de borde
 * (SaaMovilBE), nunca directamente por la app — SaaBE queda en la intranet.
 *
 * A propósito NO expone {@code getAll}/{@code getId} genéricos: la entidad guarda el hash
 * de la clave y listarla/consultarla por REST sin restricción sería exponer ese hash
 * innecesariamente. Todo el acceso pasa por los 6 verbos de negocio de abajo — y por el
 * mismo motivo, {@code crear}/{@code resetearClave}/{@code reactivar} devuelven
 * {@link UsuarioAppDTO}, nunca la entidad completa (ver {@link #aDTO}).
 *
 * La clave JAMÁS viaja en la URL — siempre en el body (a diferencia de
 * {@code usro/validaUsuario}).
 *
 * <p><b>⚠️ Desviación deliberada del estilo de la casa (decisión del usuario,
 * 2026-09-03):</b> el resto del sistema resuelve cualquier {@code Throwable} a 500. Acá
 * NO — {@link IncomeException} (siempre una falla de negocio/credenciales, nunca una
 * falla real de infraestructura en estos endpoints) se traduce a un código específico por
 * grupo, y el 500 queda reservado solo para lo que no es {@code IncomeException} (Oracle
 * caído, bug, etc.):
 * <ul>
 *   <li>{@code validarCredencial}/{@code cambiarClave}/{@code desactivar} → <b>401</b>.
 *       El body sigue siendo el mismo mensaje genérico en los cuatro casos de fondo
 *       (identificación inexistente, clave mala, BLOQUEADO, ELIMINADO) — el 401 NO
 *       desambigua cuál; solo separa "problema de credenciales" de "el servidor se
 *       rompió", para que el borde no tenga que adivinar comparando el texto del mensaje
 *       (eso se rompería el día que alguien reescriba el string).</li>
 *   <li>{@code crear}/{@code resetearClave}/{@code reactivar} → <b>400</b>, con el mensaje
 *       específico de la regla de negocio que falló (identificación inexistente en
 *       CRD.ENTD, ya tiene credencial, estado equivocado para el endpoint, etc.) — acá SÍ
 *       se puede ser específico porque son endpoints de intranet, no hay nada que
 *       ocultarle a un empleado de oficina.</li>
 * </ul>
 *
 * @author Sistema SAA
 * @since 2026-09-03
 */
@Path("usap")
public class UsuarioAppRest {

    @EJB
    private UsuarioAppService usuarioAppService;

    public UsuarioAppRest() {
    }

    /**
     * POST /rest/usap/validarCredencial — login de la app móvil. 401 (no 500) si las
     * credenciales no validan — ver el JavaDoc de la clase.
     */
    @POST
    @Path("/validarCredencial")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validarCredencial(SolicitudValidarCredencial solicitud) {
        System.out.println("LLEGA AL SERVICIO POST validarCredencial - USAP");
        try {
            ValidarCredencialResponse resultado = usuarioAppService.validarCredencial(solicitud);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al validar credenciales: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/usap/cambiarClave — el participe cambia su propia clave.
     * <p>
     * ⚠️ Dos códigos de error distintos, a propósito NO ambos 401 — ver
     * {@link ValidacionException}: si {@code claveActual} no valida → 401 (falla de
     * "quién sos"); si {@code claveNueva} no cumple la política → 400 (falla de "lo que
     * mandaste no sirve"). La app móvil trata cualquier 401 como sesión vencida y fuerza
     * el logout — devolver 401 por una clave nueva floja encerraría en un loop a un
     * partícipe recién enrolado ({@code debeCambiarClave = true}) que nunca podría activar
     * su cuenta.
     */
    @POST
    @Path("/cambiarClave")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarClave(SolicitudCambiarClaveUsuarioApp solicitud) {
        System.out.println("LLEGA AL SERVICIO POST cambiarClave - USAP");
        try {
            usuarioAppService.cambiarClave(solicitud);
            return Response.status(Response.Status.OK).entity(mensaje("Clave actualizada"))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (ValidacionException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cambiar la clave: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/usap/crear — enrolamiento desde la oficina (FE de intranet). Body:
     * {@code {identificacion, claveTemporal, usuario}} — {@code usuario} es OBLIGATORIO
     * (usuario de oficina que enrola; lo manda el FE de intranet). 400 (no 500) si la
     * identificación no existe en CRD.ENTD o el partícipe ya tiene credencial — ver el
     * JavaDoc de la clase.
     */
    @POST
    @Path("/crear")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crear(SolicitudCrearUsuarioApp solicitud) {
        System.out.println("LLEGA AL SERVICIO POST crear - USAP");
        try {
            UsuarioApp resultado = usuarioAppService.crear(solicitud);
            return Response.status(Response.Status.CREATED).entity(aDTO(resultado)).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear la credencial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/usap/resetearClave — la oficina resetea la clave de un participe. Body:
     * {@code {identificacion, claveTemporal, usuario}} — {@code usuario} es OBLIGATORIO
     * (usuario de oficina que resetea; lo manda el FE de intranet, SIEMPRE sobreescribe
     * USAPUSAR). 400 (no 500) si no existe credencial o el usuario está ELIMINADO (en ese
     * caso, usar {@code reactivar}) — ver el JavaDoc de la clase.
     */
    @POST
    @Path("/resetearClave")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetearClave(SolicitudResetearClaveUsuarioApp solicitud) {
        System.out.println("LLEGA AL SERVICIO POST resetearClave - USAP");
        try {
            UsuarioApp resultado = usuarioAppService.resetearClave(solicitud);
            return Response.status(Response.Status.OK).entity(aDTO(resultado)).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al resetear la clave: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/usap/reactivar — la oficina revierte el borrado (ELIMINADO) de una
     * cuenta. Body: {@code {identificacion, claveTemporal, usuario}} — {@code usuario} es
     * OBLIGATORIO. A propósito SEPARADO de {@code resetearClave}: solo aplica sobre una
     * cuenta ELIMINADA (se rechaza si está ACTIVA o BLOQUEADA — ese caso es
     * {@code resetearClave}), para que revertir un borrado sea un acto deliberado del
     * empleado y quede auditado. 400 (no 500) ante el rechazo — ver el JavaDoc de la clase.
     *
     * ⚠️ Endpoint de INTRANET únicamente. El WAR de borde (SaaMovilBE) NO lo expone hacia
     * internet bajo ninguna circunstancia — no forma parte del contrato con la app móvil.
     */
    @POST
    @Path("/reactivar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reactivar(SolicitudReactivarUsuarioApp solicitud) {
        System.out.println("LLEGA AL SERVICIO POST reactivar - USAP");
        try {
            UsuarioApp resultado = usuarioAppService.reactivar(solicitud);
            return Response.status(Response.Status.OK).entity(aDTO(resultado)).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al reactivar la cuenta: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/usap/desactivar — "eliminación de cuenta" exigida por Google Play y App
     * Store. 401 (no 500) si las credenciales no validan — ver el JavaDoc de la clase.
     */
    @POST
    @Path("/desactivar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response desactivar(SolicitudDesactivarUsuarioApp solicitud) {
        System.out.println("LLEGA AL SERVICIO POST desactivar - USAP");
        try {
            usuarioAppService.desactivar(solicitud);
            return Response.status(Response.Status.OK).entity(mensaje("Cuenta desactivada"))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al desactivar la cuenta: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Traduce la entidad a la vista segura de respuesta — NUNCA se serializa
     * {@code UsuarioApp} directo, para no filtrar {@code claveHash} en el body de
     * {@code crear}/{@code resetearClave}/{@code reactivar}.
     */
    private UsuarioAppDTO aDTO(UsuarioApp usap) {
        UsuarioAppDTO dto = new UsuarioAppDTO();
        dto.setCodigo(usap.getCodigo());
        dto.setIdEntidad(usap.getEntidad() != null ? usap.getEntidad().getCodigo() : null);
        dto.setIdentificacion(usap.getIdentificacion());
        dto.setEstado(usap.getEstado());
        dto.setIntentosFallidos(usap.getIntentosFallidos());
        dto.setBloqueadoHasta(usap.getBloqueadoHasta());
        dto.setDebeCambiarClave(Long.valueOf(1L).equals(usap.getDebeCambiarClave()));
        dto.setFechaCreacion(usap.getFechaCreacion());
        dto.setFechaUltimoAcceso(usap.getFechaUltimoAcceso());
        dto.setUsuarioRegistro(usap.getUsuarioRegistro());
        return dto;
    }

    /**
     * Envoltorio {@code {"mensaje": "..."}} para las respuestas de puro texto —
     * {@code Content-Type: application/json} garantizado por {@code @Produces}/{@code
     * .type(...)}, con un body JSON real (objeto con un campo), nunca un string suelto
     * cuya forma exacta al serializar dependa de qué proveedor JSON esté activo.
     */
    private Object mensaje(String texto) {
        return new Object() {
            public final String mensaje = texto;
        };
    }
}
