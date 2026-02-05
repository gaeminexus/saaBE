package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tsr.service.PersonaCuentaContableService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.PersonaCuentaContable;

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

@Path("prcc")
public class PersonaCuentaContableRest {

    @EJB
    private PersonaCuentaContableDaoService personaCuentaContableDaoService;

    @EJB
    private PersonaCuentaContableService personaCuentaContableService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public PersonaCuentaContableRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de PersonaCuentaContable.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PersonaCuentaContable> lista = personaCuentaContableDaoService.selectAll(NombreEntidadesTesoreria.PERSONA_CUENTA_CONTABLE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cuentas contables de persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de PersonaCuentaContable por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PersonaCuentaContable personaCuentaContable = personaCuentaContableDaoService.selectById(id, NombreEntidadesTesoreria.PERSONA_CUENTA_CONTABLE);
            if (personaCuentaContable == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("PersonaCuentaContable con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(personaCuentaContable).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cuenta contable de persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PersonaCuentaContable registro) {
        System.out.println("LLEGA AL SERVICIO PUT PERSONA CUENTA CONTABLE");
        try {
            PersonaCuentaContable resultado = personaCuentaContableService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cuenta contable de persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PersonaCuentaContable registro) {
        System.out.println("LLEGA AL SERVICIO POST PERSONA CUENTA CONTABLE");
        try {
            PersonaCuentaContable resultado = personaCuentaContableService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cuenta contable de persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of PersonaCuentaContableRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de PERSONA_CUENTA_CONTABLE");
        try {
            return Response.status(Response.Status.OK)
                    .entity(personaCuentaContableService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de PersonaCuentaContable por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE PERSONA CUENTA CONTABLE");
        try {
            PersonaCuentaContable elimina = new PersonaCuentaContable();
            personaCuentaContableDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cuenta contable de persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
