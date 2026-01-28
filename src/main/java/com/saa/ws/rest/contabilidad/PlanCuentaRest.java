package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.PlanCuentaDaoService;
import com.saa.ejb.contabilidad.service.PlanCuentaService;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.PlanCuenta;

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

@Path("plnn")
public class PlanCuentaRest {

    @EJB
    private PlanCuentaDaoService planCuentaDaoService;

    @EJB
    private PlanCuentaService planCuentaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public PlanCuentaRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of PlanCuentaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PlanCuenta> lista = planCuentaDaoService.selectAll(NombreEntidadesContabilidad.PLAN_CUENTA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener plan de cuentas: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PlanCuenta planCuenta = planCuentaDaoService.selectById(id, NombreEntidadesContabilidad.PLAN_CUENTA);
            if (planCuenta == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Plan de cuenta con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(planCuenta).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener plan de cuenta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
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
            List<PlanCuenta> lista = planCuentaDaoService.selectByEmpresa(idEmpresa);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener naturalezas por empresa: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    

    /**
     * PUT method for updating or creating an instance of PlanCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PlanCuenta registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PLAN_CUENTA");
        try {
            PlanCuenta resultado = planCuentaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar plan de cuenta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of PlanCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PlanCuenta registro) {
        System.out.println("LLEGA AL SERVICIO POST - PLAN_CUENTA");
        try {
            PlanCuenta resultado = planCuentaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear plan de cuenta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of PlanCuentaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de PLAN_CUENTA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(planCuentaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of PlanCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PLAN_CUENTA");
        try {
            PlanCuenta elimina = new PlanCuenta();
            planCuentaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar plan de cuenta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
