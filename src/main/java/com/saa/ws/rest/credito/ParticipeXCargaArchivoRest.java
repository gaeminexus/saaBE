package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ParticipeXCargaArchivoDaoService;
import com.saa.ejb.credito.service.ParticipeXCargaArchivoService;
import com.saa.model.credito.ParticipeXCargaArchivo;
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

@Path("pxca")
public class ParticipeXCargaArchivoRest {

    @EJB
    private ParticipeXCargaArchivoDaoService participeXCargaArchivoDaoService;

    @EJB
    private ParticipeXCargaArchivoService participeXCargaArchivoService;

    @Context
    private UriInfo context;

    public ParticipeXCargaArchivoRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ParticipeXCargaArchivo> getAll() throws Throwable {
        return participeXCargaArchivoDaoService.selectAll(NombreEntidadesCredito.PARTICIPE_X_CARGA_ARCHIVO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ParticipeXCargaArchivo getId(@PathParam("id") Long id) throws Throwable {
        return participeXCargaArchivoDaoService.selectById(id, NombreEntidadesCredito.PARTICIPE_X_CARGA_ARCHIVO);
    }

    @PUT
    @Consumes("application/json")
    public ParticipeXCargaArchivo put(ParticipeXCargaArchivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - PARTICIPEXCARGAARCHIVO");
        return participeXCargaArchivoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public ParticipeXCargaArchivo post(ParticipeXCargaArchivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - PARTICIPEXCARGAARCHIVO");
        return participeXCargaArchivoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PARTICIPEXCARGAARCHIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(participeXCargaArchivoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - PARTICIPEXCARGAARCHIVO");
        ParticipeXCargaArchivo elimina = new ParticipeXCargaArchivo();
        participeXCargaArchivoDaoService.remove(elimina, id);
    }
}
