package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.AuditoriaDaoService;
import com.saa.ejb.credito.service.AuditoriaService;
import com.saa.model.credito.Auditoria;
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


@Path("adtr")
public class AuditoriaRest {
	
	@EJB
	private AuditoriaDaoService auditoriaDaoService;
	
	@EJB
	private AuditoriaService auditoriaService;
	
	@Context
	private UriInfo context;
	
	public AuditoriaRest() {			
	}	
	
	
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<Auditoria> lista = auditoriaDaoService.selectAll(NombreEntidadesCredito.AUDITORIA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener auditorias: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
    
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
		try {
			Auditoria auditoria = auditoriaDaoService.selectById(id, NombreEntidadesCredito.AUDITORIA);
			if (auditoria == null) {
				return Response.status(Response.Status.NOT_FOUND).entity("Auditoria con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(auditoria).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener auditoria: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Auditoria registro) {
		System.out.println("LLEGA AL SERVICIO PUT - AUDITORIA");
		try {
			Auditoria resultado = auditoriaService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar auditoria: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
    }
	
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Auditoria registro) {
		System.out.println("LLEGA AL SERVICIO POST - AUDITORIA");
		try {
			Auditoria resultado = auditoriaService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear auditoria: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
		System.out.println("LLEGA AL SERVICIO POST - AUDITORIA selectByCriteria");
		Response respuesta = null;
		
		try {
			respuesta = Response
					.status(Response.Status.OK)
					.entity(auditoriaService.selectByCriteria(registros))
					.type(MediaType.APPLICATION_JSON).build();		
			} catch (Throwable th) {
				respuesta = Response
					.status(Response.Status.BAD_REQUEST)
					.entity(th.getMessage())
					.type(MediaType.APPLICATION_JSON).build();				
			}
		return respuesta;
	}
    
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
    	System.out.println("LLEGA AL SERVICIO DELETE - AUDITORIA");
		try {
			Auditoria entidad = new Auditoria();
			auditoriaDaoService.remove(entidad, id);
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar auditoria: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
    }

}
