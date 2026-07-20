package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.DetalleCargaTxtDaoService;
import com.saa.ejb.cxp.service.DetalleCargaTxtService;
import com.saa.model.cxp.DetalleCargaTxt;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("dctx")
public class DetalleCargaTxtRest {
	@EJB private DetalleCargaTxtDaoService detalleCargaTxtDaoService;
	@EJB private DetalleCargaTxtService detalleCargaTxtService;
	@Context private UriInfo context;
	public DetalleCargaTxtRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<DetalleCargaTxt> lista = detalleCargaTxtDaoService.selectAll(NombreEntidadesCompra.DETALLE_CARGA_TXT);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			DetalleCargaTxt entidad = detalleCargaTxtDaoService.selectById(id, NombreEntidadesCompra.DETALLE_CARGA_TXT);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("DetalleCargaTxt ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> registros) {
		System.out.println("selectByCriteria de DetalleCargaTxt");
		try {
			return Response.status(Response.Status.OK)
					.entity(detalleCargaTxtService.selectByCriteria(registros))
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	/**
	 * Obtiene todas las líneas de una carga específica.
	 * Cada línea incluye el DocumentoCxp embebido con su estado actual.
	 * GET /dctx/getByCarga/{idCarga}
	 */
	@GET @Path("/getByCarga/{idCarga}") @Produces(MediaType.APPLICATION_JSON)
	public Response getByCarga(@PathParam("idCarga") Long idCarga) {
		try {
			List<DetalleCargaTxt> lista = detalleCargaTxtService.selectByCarga(idCarga);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	/**
	 * Obtiene todas las cargas en las que apareció un documento específico.
	 * Útil para ver el historial de cargas de un documento.
	 * GET /dctx/getByDocumento/{idDocumento}
	 */
	@GET @Path("/getByDocumento/{idDocumento}") @Produces(MediaType.APPLICATION_JSON)
	public Response getByDocumento(@PathParam("idDocumento") Long idDocumento) {
		try {
			List<DetalleCargaTxt> lista = detalleCargaTxtService.selectByDocumento(idDocumento);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(DetalleCargaTxt registro) {
		try {
			DetalleCargaTxt resultado = detalleCargaTxtService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(DetalleCargaTxt registro) {
		try {
			DetalleCargaTxt resultado = detalleCargaTxtService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE - DetalleCargaTxt");
		try {
			DetalleCargaTxt elimina = new DetalleCargaTxt();
			detalleCargaTxtDaoService.remove(elimina, id);
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
