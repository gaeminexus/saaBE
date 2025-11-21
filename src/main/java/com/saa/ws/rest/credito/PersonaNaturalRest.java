package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.PersonaNaturalDaoService;
import com.saa.ejb.credito.service.PersonaNaturalService;
import com.saa.model.credito.PersonaNatural;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("prsn")
public class PersonaNaturalRest {

    @EJB
    private PersonaNaturalDaoService personaNaturalDaoService;

    @EJB
    private PersonaNaturalService personaNaturalService;

    @Context
    private UriInfo context;

    public PersonaNaturalRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<PersonaNatural> getAll() throws Throwable {
        return personaNaturalDaoService.selectAll(NombreEntidadesCredito.PERSONA_NATURAL);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public PersonaNatural getId(@PathParam("id") Long id) throws Throwable {
        return personaNaturalDaoService.selectById(id, NombreEntidadesCredito.PERSONA_NATURAL);
    }

    @PUT
    @Consumes("application/json")
    public PersonaNatural put(PersonaNatural registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - PERSONA NATURAL");
        return personaNaturalService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public PersonaNatural post(PersonaNatural registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - PERSONA NATURAL");
        return personaNaturalService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PERSONA NATURAL");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(personaNaturalService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - PERSONA NATURAL");
        PersonaNatural elimina = new PersonaNatural();
        personaNaturalDaoService.remove(elimina, id);
    }

}
