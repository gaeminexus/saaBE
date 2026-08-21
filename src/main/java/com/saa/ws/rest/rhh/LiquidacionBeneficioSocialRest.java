package com.saa.ws.rest.rhh;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService;
import com.saa.ejb.rhh.service.BeneficioSocialService;
import com.saa.ejb.rhh.service.LiquidacionBeneficioSocialService;
import com.saa.model.rhh.LiquidacionBeneficioSocial;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("lqbs")
public class LiquidacionBeneficioSocialRest {

    @EJB
    private LiquidacionBeneficioSocialDaoService liquidacionBeneficioSocialDaoService;

    @EJB
    private LiquidacionBeneficioSocialService liquidacionBeneficioSocialService;

    @EJB
    private BeneficioSocialService beneficioSocialService;

    @Context
    private UriInfo context;

    public LiquidacionBeneficioSocialRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - LIQUIDACIONBENEFICIOSOCIAL");
        try {
            List<LiquidacionBeneficioSocial> lista = liquidacionBeneficioSocialDaoService.selectAll(NombreEntidadesRhh.LIQUIDACION_BENEFICIO_SOCIAL);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - LIQUIDACIONBENEFICIOSOCIAL");
        try {
            LiquidacionBeneficioSocial registro = liquidacionBeneficioSocialDaoService.selectById(id, NombreEntidadesRhh.LIQUIDACION_BENEFICIO_SOCIAL);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Registro con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(LiquidacionBeneficioSocial registro) {
        System.out.println("LLEGA AL SERVICIO PUT - LIQUIDACIONBENEFICIOSOCIAL");
        try {
            LiquidacionBeneficioSocial actualizado = liquidacionBeneficioSocialService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(LiquidacionBeneficioSocial registro) {
        System.out.println("LLEGA AL SERVICIO POST - LIQUIDACIONBENEFICIOSOCIAL");
        try {
            LiquidacionBeneficioSocial creado = liquidacionBeneficioSocialService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(creado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de LIQUIDACIONBENEFICIOSOCIAL");
        try {
            List<LiquidacionBeneficioSocial> lista = liquidacionBeneficioSocialService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - LIQUIDACIONBENEFICIOSOCIAL");
        try {
            liquidacionBeneficioSocialService.remove(List.of(id));
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Endpoints de proceso de beneficios sociales
    // =====================================================================

    /**
     * Genera el decimo tercero del anio para todos los empleados con contrato activo.
     * Es idempotente: si el beneficio ya existe se actualiza.
     */
    @POST
    @Path("/generarDecimoTercero/{anio}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarDecimoTercero(@PathParam("anio") Integer anio,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("usuarioRegistro") String usuario) {
        System.out.println("LLEGA AL SERVICIO GENERAR DECIMO TERCERO - LIQUIDACIONBENEFICIOSOCIAL, anio: " + anio);
        try {
            int generados = beneficioSocialService.generarDecimoTercero(idEmpresa, anio, usuario);
            return Response.status(Response.Status.OK).entity(generados).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al generar el decimo tercero: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Genera el decimo cuarto del anio para los empleados de una region. El periodo de
     * acumulacion depende de ella, por eso va en la ruta.
     */
    @POST
    @Path("/generarDecimoCuarto/{anio}/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarDecimoCuarto(@PathParam("anio") Integer anio,
            @PathParam("region") Integer region,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("usuarioRegistro") String usuario) {
        System.out.println("LLEGA AL SERVICIO GENERAR DECIMO CUARTO - LIQUIDACIONBENEFICIOSOCIAL, anio: "
                + anio + ", region: " + region);
        try {
            int generados = beneficioSocialService.generarDecimoCuarto(idEmpresa, anio, region, usuario);
            return Response.status(Response.Status.OK).entity(generados).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al generar el decimo cuarto: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Genera los fondos de reserva acumulados del anio, solo para los contratos en
     * modalidad acumulada en el IESS.
     */
    @POST
    @Path("/generarFondosReserva/{anio}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarFondosReserva(@PathParam("anio") Integer anio,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("usuarioRegistro") String usuario) {
        System.out.println("LLEGA AL SERVICIO GENERAR FONDOS DE RESERVA - LIQUIDACIONBENEFICIOSOCIAL, anio: " + anio);
        try {
            int generados = beneficioSocialService.generarFondosReserva(idEmpresa, anio, usuario);
            return Response.status(Response.Status.OK).entity(generados).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al generar los fondos de reserva: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
