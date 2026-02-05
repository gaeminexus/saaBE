package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.AnioMotorDaoService;
import com.saa.ejb.cnt.service.AnioMotorService;
import com.saa.model.cnt.AnioMotor;
import com.saa.model.cnt.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("anio")
public class AnioMortorRest {

    @EJB
    private AnioMotorDaoService anioMotorDaoService;

    @EJB
    private AnioMotorService anioMotorService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AnioMortorRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of AnioMortorRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AnioMotor> lista = anioMotorDaoService.selectAll(NombreEntidadesContabilidad.ANIO_MOTOR);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener años motor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getDesc")
    public Response getDesc() {
        try {
            List<AnioMotor> lista = anioMotorDaoService.selectOrderDesc();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener años motor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ...existing code...

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            AnioMotor anioMotor = anioMotorDaoService.selectById(id, NombreEntidadesContabilidad.ANIO_MOTOR);
            if (anioMotor == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("AnioMotor con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(anioMotor).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener año motor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(AnioMotor registro) {
        System.out.println("LLEGA AL SERVICIO PUT - ANIO_MOTOR");
        try {
            AnioMotor resultado = anioMotorService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar año motor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(AnioMotor registro) {
        System.out.println("LLEGA AL SERVICIO POST - ANIO_MOTOR");
        try {
            AnioMotor resultado = anioMotorService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear año motor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de ANIO_MOTOR");
        try {
            return Response.status(Response.Status.OK)
                    .entity(anioMotorService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - ANIO_MOTOR");
        try {
            AnioMotor elimina = new AnioMotor();
            anioMotorDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar año motor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
