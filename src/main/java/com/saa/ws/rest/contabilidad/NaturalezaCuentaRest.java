package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.NaturalezaCuentaDaoService;
import com.saa.ejb.contabilidad.service.NaturalezaCuentaService;
import com.saa.model.cnt.NaturalezaCuenta;
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

@Path("ntrl")
public class NaturalezaCuentaRest {

    @EJB
    private NaturalezaCuentaDaoService naturalezaCuentaDaoService;

    @EJB
    private NaturalezaCuentaService naturalezaCuentaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public NaturalezaCuentaRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of NaturalezaCuentaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NaturalezaCuenta> lista = naturalezaCuentaDaoService.selectAll(NombreEntidadesContabilidad.NATURALEZA_CUENTA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener naturalezas de cuenta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

	/**
	 * Retrieves representation of an instance of NaturalezaCuentaRest
	 * 
	 * @return an instance of String
	 * @throws Throwable
	 */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            NaturalezaCuenta naturaleza = naturalezaCuentaDaoService.selectById(id, NombreEntidadesContabilidad.NATURALEZA_CUENTA);
            if (naturaleza == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Naturaleza de cuenta con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(naturaleza).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener naturaleza de cuenta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
	 * Retrieves representation of an instance of NaturalezaCuentaRest
	 * 
	 * @return an instance of String
	 * @throws Throwable
	 */
    @GET
    @Path("/getByEmpresa/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        try {
            List<NaturalezaCuenta> lista = naturalezaCuentaDaoService.selectByEmpresa(idEmpresa);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener naturalezas por empresa: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
	 * Retrieves representation of an instance of NaturalezaCuentaRest
	 * 
	 * @return an instance of String
	 * @throws Throwable
	 */
    @GET
    @Path("/validaTieneCuentas/{idNaturaleza}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validaTieneCuentas(@PathParam("idNaturaleza") Long idNaturaleza) {
        try {
            Long resultado = naturalezaCuentaService.validaTieneCuentas(idNaturaleza);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al validar cuentas: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT method for updating or creating an instance of NaturalezaCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(NaturalezaCuenta registro) {
        System.out.println("LLEGA AL SERVICIO PUT - NATURALEZA_CUENTA");
        try {
            NaturalezaCuenta resultado = naturalezaCuentaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar naturaleza de cuenta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of NaturalezaCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(NaturalezaCuenta registro) {
        System.out.println("LLEGA AL SERVICIO POST - NATURALEZA_CUENTA");
        try {
            NaturalezaCuenta resultado = naturalezaCuentaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear naturaleza de cuenta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of NaturalezaCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de NATURALEZA_CUENTA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(naturalezaCuentaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of NaturalezaCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - NATURALEZA_CUENTA");
        try {
            String resultado = naturalezaCuentaService.eliminaNaturalezaCuenta(id);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar naturaleza de cuenta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @POST
    @Path("inactivaNaturalezaCuenta")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response inactivaNaturalezaCuenta(Long idNaturaleza) {
        System.out.println("inactivaNaturalezaCuenta de NATURALEZA_CUENTA: " + idNaturaleza);
        try {
            return Response.status(Response.Status.OK)
                    .entity(naturalezaCuentaService.inactivaNaturalezaCuenta(idNaturaleza))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

}
