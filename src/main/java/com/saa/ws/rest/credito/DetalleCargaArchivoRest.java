package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.DetalleCargaArchivoDaoService;
import com.saa.ejb.credito.service.DetalleCargaArchivoService;
import com.saa.model.credito.DetalleCargaArchivo;
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

@Path("dtca")
public class DetalleCargaArchivoRest {

    @EJB
    private DetalleCargaArchivoDaoService detalleCargaArchivoDaoService;

    @EJB
    private DetalleCargaArchivoService detalleCargaArchivoService;

    @Context
    private UriInfo context;

    public DetalleCargaArchivoRest() {}

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleCargaArchivo> getAll() throws Throwable {
        return detalleCargaArchivoDaoService.selectAll(NombreEntidadesCredito.DETALLE_CARGA_ARCHIVO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DetalleCargaArchivo getId(@PathParam("id") Long id) throws Throwable {
        return detalleCargaArchivoDaoService.selectById(id, NombreEntidadesCredito.DETALLE_CARGA_ARCHIVO);
    }

    @PUT
    @Consumes("application/json")
    public DetalleCargaArchivo put(DetalleCargaArchivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLECARGAARCHIVO");
        return detalleCargaArchivoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public DetalleCargaArchivo post(DetalleCargaArchivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - DETALLECARGAARCHIVO");
        return detalleCargaArchivoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLECARGAARCHIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detalleCargaArchivoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLECARGAARCHIVO");
        DetalleCargaArchivo elimina = new DetalleCargaArchivo();
        detalleCargaArchivoDaoService.remove(elimina, id);
    }
}
