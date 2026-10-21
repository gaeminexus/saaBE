package com.saa.ws.movil;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.UsuarioAppService;
import com.saa.ejb.crd.service.ValidacionException;
import com.saa.ejb.crd.service.dto.SolicitudCambiarClaveUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudConfirmarRecuperacionUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudDesactivarUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudSolicitarRecuperacionUsuarioApp;
import com.saa.ejb.crd.service.dto.SolicitudValidarCredencial;
import com.saa.ejb.crd.service.dto.ValidarCredencialResponse;
import com.saa.ws.movil.dto.MensajeMovilDTO;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code /movil/auth/...} — §5.1 del contrato. Delega ÍNTEGRAMENTE en {@link UsuarioAppService},
 * sin duplicar una línea de lógica de negocio: solo repite la capa JAX-RS de
 * {@code com.saa.ws.rest.crd.UsuarioAppRest} bajo el path {@code /movil}, con exactamente el
 * mismo mapeo de códigos de error (ver el JavaDoc de esa clase para el porqué del 401/400).
 *
 * <p>{@code crear}/{@code resetearClave}/{@code reactivar} de USAP NO se replican acá — son de
 * oficina y quedan solo en {@code /rest/usap} (§1 y §5.1 del contrato).</p>
 */
@ClaveMovilRequerida
@Path("auth")
public class AuthMovilRest {

    private static final String MSG_SOLICITUD_RECUPERACION = "Si tu cedula tiene cuenta en la app y un correo "
            + "personal registrado en ASOPREP, te enviamos un codigo. Si no te llega, acercate a la oficina.";

    @EJB
    private UsuarioAppService usuarioAppService;

    /** POST /movil/auth/validarCredencial — login de la app móvil. */
    @POST
    @Path("/validarCredencial")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validarCredencial(SolicitudValidarCredencial solicitud) {
        System.out.println("LLEGA AL SERVICIO POST validarCredencial - MOVIL");
        try {
            ValidarCredencialResponse resultado = usuarioAppService.validarCredencial(solicitud);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(mensaje("Error al validar credenciales"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** POST /movil/auth/cambiarClave — el partícipe cambia su propia clave. */
    @POST
    @Path("/cambiarClave")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarClave(SolicitudCambiarClaveUsuarioApp solicitud) {
        System.out.println("LLEGA AL SERVICIO POST cambiarClave - MOVIL");
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
                    .entity(mensaje("Error al cambiar la clave"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** POST /movil/auth/desactivar — "eliminación de cuenta" exigida por Google Play y App Store. */
    @POST
    @Path("/desactivar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response desactivar(SolicitudDesactivarUsuarioApp solicitud) {
        System.out.println("LLEGA AL SERVICIO POST desactivar - MOVIL");
        try {
            usuarioAppService.desactivar(solicitud);
            return Response.status(Response.Status.OK).entity(mensaje("Cuenta desactivada"))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(mensaje("Error al desactivar la cuenta"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /movil/auth/solicitarRecuperacion — "olvidé mi clave", paso 1. SIEMPRE el mismo 200 y el
     * mismo texto (haya o no cuenta, correo, freno o fallo del SMTP): el texto vive aquí, en un
     * solo lugar. Solo una falla real (Oracle caído) da 500.
     */
    @POST
    @Path("/solicitarRecuperacion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response solicitarRecuperacion(SolicitudSolicitarRecuperacionUsuarioApp solicitud) {
        System.out.println("LLEGA AL SERVICIO POST solicitarRecuperacion - MOVIL");
        try {
            usuarioAppService.solicitarRecuperacion(solicitud);
            return Response.status(Response.Status.OK).entity(mensaje(MSG_SOLICITUD_RECUPERACION))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            System.err.println("solicitarRecuperacion - MOVIL: " + e.getClass().getName() + ": " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(mensaje("Error al solicitar la recuperacion"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /movil/auth/confirmarRecuperacion — "olvidé mi clave", paso 2. 400 (nunca 401: la app
     * lo tomaría como sesión vencida) con el mensaje genérico del código o el de la regla de clave.
     */
    @POST
    @Path("/confirmarRecuperacion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmarRecuperacion(SolicitudConfirmarRecuperacionUsuarioApp solicitud) {
        System.out.println("LLEGA AL SERVICIO POST confirmarRecuperacion - MOVIL");
        try {
            usuarioAppService.confirmarRecuperacion(solicitud);
            return Response.status(Response.Status.OK).entity(mensaje("Clave actualizada"))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(mensaje(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            System.err.println("confirmarRecuperacion - MOVIL: " + e.getClass().getName() + ": " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(mensaje("Error al confirmar la recuperacion"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private MensajeMovilDTO mensaje(String texto) {
        return new MensajeMovilDTO(texto);
    }
}
