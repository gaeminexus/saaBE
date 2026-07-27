/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.ejb.tsr.service.ConciliacionContableMatchService;
import com.saa.ejb.tsr.service.ConciliacionContableService;
import com.saa.ejb.tsr.service.ControlExtractoBancarioService;
import com.saa.ejb.tsr.service.GrupoConciliacionContableService;
import com.saa.model.tsr.ConciliacionContable;
import com.saa.model.tsr.GrupoConciliacionContable;
import com.saa.model.tsr.ResumenConciliacionCuenta;
import com.saa.model.tsr.SolicitudConciliarGrupo;
import com.saa.model.tsr.SolicitudUsuario;
import com.saa.model.tsr.SugerenciaConciliacionContable;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author GaemiSoft
 * <p>REST endpoint para la conciliación contable (extracto bancario vs.
 * contabilidad). Path: /cnct</p>
 */
@Path("cnct")
public class ConciliacionContableRest {

    @EJB
    private ConciliacionContableService conciliacionContableService;

    @EJB
    private ConciliacionContableMatchService conciliacionContableMatchService;

    @EJB
    private GrupoConciliacionContableService grupoConciliacionContableService;

    @EJB
    private ControlExtractoBancarioService controlExtractoBancarioService;

    /**
     * Recupera (o crea si es la primera vez) la cabecera de conciliación
     * contable para una cuenta bancaria y período.
     */
    @GET
    @Path("/cabecera/{idCuentaBancaria}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cabecera(@PathParam("idCuentaBancaria") Long idCuentaBancaria,
            @PathParam("idPeriodo") Long idPeriodo) {
        try {
            ConciliacionContable resultado = conciliacionContableService.obtenerOCrear(idCuentaBancaria, idPeriodo);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cabecera de conciliacion contable: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Filas de DetalleExtractoBancario pendientes de conciliar.
     */
    @GET
    @Path("/pendientesExtracto/{idCuentaBancaria}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pendientesExtracto(@PathParam("idCuentaBancaria") Long idCuentaBancaria,
            @PathParam("idPeriodo") Long idPeriodo) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(conciliacionContableMatchService.obtenerPendientesExtracto(idCuentaBancaria, idPeriodo))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener pendientes de extracto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Filas de DetalleAsiento (cuenta contable del banco) pendientes de conciliar.
     */
    @GET
    @Path("/pendientesAsiento/{idCuentaBancaria}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pendientesAsiento(@PathParam("idCuentaBancaria") Long idCuentaBancaria,
            @PathParam("idPeriodo") Long idPeriodo) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(conciliacionContableMatchService.obtenerPendientesAsiento(idCuentaBancaria, idPeriodo))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener pendientes de asiento: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Resumen de estado de conciliación de todas las cuentas bancarias
     * activas de una empresa para un período - alimenta la lista de
     * transparencia que se muestra al elegir el período, antes de entrar a
     * conciliar una cuenta específica. De solo lectura (no crea cabeceras).
     */
    @GET
    @Path("/resumenPorPeriodo/{idEmpresa}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumenPorPeriodo(@PathParam("idEmpresa") Long idEmpresa,
            @PathParam("idPeriodo") Long idPeriodo) {
        try {
            List<ResumenConciliacionCuenta> resumen = conciliacionContableMatchService
                    .resumenPorPeriodo(idEmpresa, idPeriodo);
            return Response.status(Response.Status.OK).entity(resumen).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el resumen por periodo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Grupos de conciliación ya vigentes (activos) de una cabecera.
     */
    @GET
    @Path("/grupos/{idConciliacionContable}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response grupos(@PathParam("idConciliacionContable") Long idConciliacionContable) {
        try {
            List<GrupoConciliacionContable> grupos = grupoConciliacionContableService
                    .selectActivosByConciliacion(idConciliacionContable);
            return Response.status(Response.Status.OK).entity(grupos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener grupos de conciliacion: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Corre la pasada de auto-conciliación (ver
     * ConciliacionContableMatchService.sugerirCoincidencias) y devuelve las
     * sugerencias encontradas, sin persistir nada todavia.
     */
    @GET
    @Path("/sugerencias/{idCuentaBancaria}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sugerencias(@PathParam("idCuentaBancaria") Long idCuentaBancaria,
            @PathParam("idPeriodo") Long idPeriodo) {
        try {
            List<SugerenciaConciliacionContable> sugerencias = conciliacionContableMatchService
                    .sugerirCoincidencias(idCuentaBancaria, idPeriodo);
            return Response.status(Response.Status.OK).entity(sugerencias).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al sugerir coincidencias: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un grupo de conciliación con las filas indicadas de ambos lados
     * (ver ConciliacionContableMatchService.conciliarGrupo para las
     * validaciones de monto y fecha).
     */
    @POST
    @Path("/conciliar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response conciliar(SolicitudConciliarGrupo solicitud) {
        try {
            if (solicitud == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Solicitud vacia").type(MediaType.APPLICATION_JSON).build();
            }
            GrupoConciliacionContable resultado = conciliacionContableMatchService.conciliarGrupo(
                    solicitud.getIdCuentaBancaria(), solicitud.getIdPeriodo(), solicitud.getIdsDetalleExtracto(),
                    solicitud.getIdsDetalleAsiento(), solicitud.getUsuario());
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al conciliar: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Deshace un grupo ya conciliado (vuelve sus filas al pool de pendientes).
     */
    @POST
    @Path("/deshacer/{idGrupo}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deshacer(@PathParam("idGrupo") Long idGrupo, SolicitudUsuario solicitud) {
        try {
            String usuario = solicitud != null ? solicitud.getUsuario() : null;
            conciliacionContableMatchService.deshacerGrupo(idGrupo, usuario);
            return Response.status(Response.Status.OK).entity("Grupo deshecho correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al deshacer el grupo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Marca una cuenta/período como verificado (solo si no quedan pendientes).
     */
    @POST
    @Path("/verificar/{idConciliacionContable}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificar(@PathParam("idConciliacionContable") Long idConciliacionContable,
            SolicitudUsuario solicitud) {
        try {
            String usuario = solicitud != null ? solicitud.getUsuario() : null;
            conciliacionContableService.verificar(idConciliacionContable, usuario);
            return Response.status(Response.Status.OK).entity("Cuenta/periodo verificado correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al verificar: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Cierra el período (todo el mes), solo si todas las cuentas bancarias
     * activas de la empresa ya están verificadas para ese período.
     */
    @POST
    @Path("/cerrarMes/{idEmpresa}/{idPeriodo}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cerrarMes(@PathParam("idEmpresa") Long idEmpresa, @PathParam("idPeriodo") Long idPeriodo,
            SolicitudUsuario solicitud) {
        try {
            String usuario = solicitud != null ? solicitud.getUsuario() : null;
            conciliacionContableMatchService.cerrarMes(idEmpresa, idPeriodo, usuario);
            return Response.status(Response.Status.OK).entity("Mes cerrado correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cerrar el mes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Reabre un período previamente cerrado.
     */
    @POST
    @Path("/reabrirMes/{idEmpresa}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reabrirMes(@PathParam("idEmpresa") Long idEmpresa, @PathParam("idPeriodo") Long idPeriodo) {
        try {
            conciliacionContableMatchService.reabrirMes(idEmpresa, idPeriodo);
            return Response.status(Response.Status.OK).entity("Mes reabierto correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al reabrir el mes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Ids de todos los períodos ya cerrados (para conciliación bancaria, ver
     * ControlExtractoBancario.cerrado) de una empresa - usado por pantallas
     * que necesitan marcar varios períodos a la vez (ej. deshabilitar
     * opciones en un selector) sin hacer una llamada por período.
     */
    @GET
    @Path("/periodosCerrados/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response periodosCerrados(@PathParam("idEmpresa") Long idEmpresa) {
        try {
            List<Long> cerrados = controlExtractoBancarioService.selectPeriodosCerrados(idEmpresa);
            return Response.status(Response.Status.OK).entity(cerrados).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al consultar los periodos cerrados: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
