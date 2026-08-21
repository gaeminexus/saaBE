package com.saa.ws.rest.rhh;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.LiquidacionDaoService;
import com.saa.ejb.rhh.service.ContabilizacionNominaService;
import com.saa.ejb.rhh.service.LiquidacionHaberesService;
import com.saa.ejb.rhh.service.LiquidacionService;
import com.saa.model.rhh.Liquidacion;
import com.saa.model.cnt.Asiento;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.ResultadoLiquidacion;

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

@Path("lqdc")
public class LiquidacionRest {

    @EJB
    private LiquidacionDaoService LiquidacionDaoService;

    @EJB
    private LiquidacionService LiquidacionService;

    @EJB
    private LiquidacionHaberesService liquidacionHaberesService;

    @EJB
    private ContabilizacionNominaService contabilizacionNominaService;

    @Context
    private UriInfo context;

    public LiquidacionRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Liquidacion> lista = LiquidacionDaoService.selectAll(NombreEntidadesRhh.LIQUIDACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Liquidacion registro = LiquidacionDaoService.selectById(id, NombreEntidadesRhh.LIQUIDACION);
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
    public Response put(Liquidacion registro) {
        System.out.println("LLEGA AL SERVICIO PUT - LIQUIDACION");
        try {
            Liquidacion actualizado = LiquidacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Liquidacion registro) {
        System.out.println("LLEGA AL SERVICIO POST - LIQUIDACION");
        try {
            Liquidacion creado = LiquidacionService.saveSingle(registro);
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
        System.out.println("selectByCriteria de LIQUIDACION");
        try {
            List<Liquidacion> lista = LiquidacionService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en búsqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - LIQUIDACION");
        try {
            Liquidacion elimina = new Liquidacion();
            LiquidacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    // =====================================================================
    // Endpoints de proceso - fase 8
    // =====================================================================

    /**
     * Calcula el finiquito sin persistir nada, para que el usuario lo revise.
     */
    @POST
    @Path("/simular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response simular(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO simular - LIQUIDACION");
        try {
            ResultadoLiquidacion resultado = liquidacionHaberesService.simular(
                    leeLong(datos, "idContrato"), leeFecha(datos, "fechaSalida"),
                    leeLong(datos, "idCausal"));
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al simular la liquidacion: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Calcula el finiquito y lo persiste con sus rubros.
     */
    @POST
    @Path("/calcular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response calcular(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO calcular - LIQUIDACION");
        try {
            Liquidacion liquidacion = liquidacionHaberesService.calcular(
                    leeLong(datos, "idContrato"), leeFecha(datos, "fechaSalida"),
                    leeLong(datos, "idCausal"), leeTexto(datos, "observaciones"),
                    leeTexto(datos, "usuarioRegistro"));
            return Response.status(Response.Status.OK).entity(liquidacion).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al calcular la liquidacion: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Aprueba la liquidacion. Desde aqui ya no se recalcula.
     */
    @POST
    @Path("/aprobar/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response aprobar(@PathParam("id") Long id,
            @QueryParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO aprobar - LIQUIDACION, liquidacion: " + id);
        try {
            liquidacionHaberesService.aprobar(id, usuarioRegistro);
            return Response.status(Response.Status.OK).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al aprobar la liquidacion: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Ejecuta la salida: cierra el contrato, cesa al empleado, avisa al IESS, cancela los
     * descuentos y caduca los saldos de vacaciones.
     */
    @POST
    @Path("/ejecutarSalida/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ejecutarSalida(@PathParam("id") Long id,
            @QueryParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO ejecutarSalida - LIQUIDACION, liquidacion: " + id);
        try {
            liquidacionHaberesService.ejecutarSalida(id, usuarioRegistro);
            return Response.status(Response.Status.OK).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al ejecutar la salida: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Contabiliza la liquidacion aprobada.
     */
    @POST
    @Path("/contabilizar/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contabilizar(@PathParam("id") Long id,
            @QueryParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO contabilizar - LIQUIDACION, liquidacion: " + id);
        try {
            Asiento asiento = contabilizacionNominaService.contabilizarLiquidacion(id, usuarioRegistro);
            return Response.status(Response.Status.OK).entity(asiento).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al contabilizar la liquidacion: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Lectura del cuerpo
    // =====================================================================

    private Long leeLong(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null ? Long.valueOf(valor.toString()) : null;
    }

    private String leeTexto(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null ? valor.toString() : null;
    }

    private LocalDate leeFecha(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null ? LocalDate.parse(valor.toString()) : null;
    }
}
