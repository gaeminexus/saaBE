package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.CentroCostoDaoService;
import com.saa.ejb.contabilidad.service.CentroCostoService;
import com.saa.model.contabilidad.CentroCosto;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;

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

@Path("cncs")
public class CentroCostoRest {

    @EJB
    private CentroCostoDaoService centroCostoDaoService;

    @EJB
    private CentroCostoService centroCostoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public CentroCostoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of CentroCostoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CentroCosto> lista = centroCostoDaoService.selectAll(NombreEntidadesContabilidad.CENTRO_COSTO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener centros de costo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            CentroCosto centroCosto = centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO);
            if (centroCosto == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CentroCosto con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(centroCosto).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener centro de costo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/validaExistenAsientos/{idCentroCosto}")
    public Response validaExistenAsientos(@PathParam("idCentroCosto") Long idCentroCosto) {
        try {
            Long resultado = centroCostoService.validaExistenAsientos(idCentroCosto);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al validar asientos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CentroCosto registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CENTRO_COSTO");
        try {
            CentroCosto resultado = centroCostoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar centro de costo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CentroCosto registro) {
        System.out.println("LLEGA AL SERVICIO POST - CENTRO_COSTO");
        try {
            CentroCosto resultado = centroCostoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear centro de costo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CENTRO_COSTO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(centroCostoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - CENTRO_COSTO");
        try {
            CentroCosto elimina = new CentroCosto();
            centroCostoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar centro de costo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}


