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


@Path("auditoria")
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
    @Produces("application/json")
	public List<Auditoria> getAll() throws Throwable {
		return auditoriaDaoService.selectAll(NombreEntidadesCredito.AUDITORIA);
	}
    
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Auditoria getId(@PathParam("id") Long id) throws Throwable {
		return auditoriaDaoService.selectById(id, NombreEntidadesCredito.AUDITORIA);
    }
    
    @PUT
    @Consumes("application/json")
    public Auditoria put(Auditoria registro) throws Throwable {
		System.out.println("LLEGA AL SERVICIO PUT - AUDITORIA");
		return auditoriaService.saveSingle(registro);
    }
	
    @POST
    @Consumes("application/json")
    public Auditoria post(Auditoria registro) throws Throwable {
		System.out.println("LLEGA AL SERVICIO POST - AUDITORIA");
		return auditoriaService.saveSingle(registro);
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
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
    	System.out.println("LLEGA AL SERVICIO DELETE - AUDITORIA");
    	Auditoria entidad = new Auditoria();
    	auditoriaDaoService.remove(entidad, id);
    }

}
