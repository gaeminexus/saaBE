package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.CargaArchivoDaoService;
import com.saa.ejb.crd.service.CargaArchivoService;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.NombreEntidadesCredito;

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

@Path("crar")
public class CargaArchivoRest {

    @EJB
    private CargaArchivoDaoService cargaArchivoDaoService;

    @EJB
    private CargaArchivoService cargaArchivoService;
    
    @Context
    private UriInfo context;

    public CargaArchivoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CargaArchivo> lista = cargaArchivoDaoService.selectAll(NombreEntidadesCredito.CARGA_ARCHIVO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cargas de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CargaArchivo cargaArchivo = cargaArchivoDaoService.selectById(id, NombreEntidadesCredito.CARGA_ARCHIVO);
            if (cargaArchivo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CargaArchivo con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cargaArchivo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener carga de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Path("/melyTest/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public String melyTest(@PathParam("idEntidad") Long idEntidad) throws Throwable {
        String nombre = "";
        nombre = cargaArchivoService.melyTest(idEntidad);
        nombre = "{\"nombre\": \"" + nombre + "\"}";
        System.out.println("nombre retornado en el rest: " + nombre);
        return nombre;
    }
    
    @GET
    @Path("/getByAnio/{anio}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByAnio(@PathParam("anio") Long anio) {
        try {
            List<CargaArchivo> lista = cargaArchivoDaoService.selectByAnio(anio);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cargas por año: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CargaArchivo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CARGAARCHIVO");
        try {
            CargaArchivo resultado = cargaArchivoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar carga de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CargaArchivo registro) {
        System.out.println("LLEGA AL SERVICIO POST - CARGAARCHIVO");
        try {
            CargaArchivo resultado = cargaArchivoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear carga de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CARGAARCHIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cargaArchivoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - CARGAARCHIVO");
        try {
            CargaArchivo elimina = new CargaArchivo();
            cargaArchivoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar carga de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
