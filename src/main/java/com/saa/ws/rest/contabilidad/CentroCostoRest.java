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
    @Produces("application/json")
    public List<CentroCosto> getAll() throws Throwable {
        return centroCostoDaoService.selectAll(NombreEntidadesContabilidad.CENTRO_COSTO);
    }


    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public CentroCosto getId(@PathParam("id") Long id) throws Throwable {
        return centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO);
    }
    
    @GET
    @Produces("application/json")
    @Path("/validaExistenAsientos/{idCentroCosto}")
    public Long validaExistenAsientos(@PathParam("idCentroCosto") Long idCentroCosto) throws Throwable {
        return centroCostoService.validaExistenAsientos(idCentroCosto);
    }

    /**
     * PUT method for updating or creating an instance of CentroCostoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public CentroCosto put(CentroCosto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return centroCostoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CentroCostoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public CentroCosto post(CentroCosto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return centroCostoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CentroCostoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CENTRO_COSTO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(centroCostoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
    /**
     * POST method for updating or creating an instance of CentroCostoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        CentroCosto elimina = new CentroCosto();
        centroCostoDaoService.remove(elimina, id);
    }

}


