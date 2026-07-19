package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.RetencionCompraV2DaoService;
import com.saa.ejb.cxp.service.RetencionCompraV2Service;
import com.saa.model.cxp.RetencionCompraV2;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("rcv2")
public class RetencionCompraV2Rest {
	@EJB private RetencionCompraV2DaoService retencionCompraV2DaoService;
	@EJB private RetencionCompraV2Service retencionCompraV2Service;
	@Context private UriInfo context;
	public RetencionCompraV2Rest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<RetencionCompraV2> lista = retencionCompraV2DaoService.selectAll(NombreEntidadesCompra.RETENCION_COMPRA_V2);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			RetencionCompraV2 entidad = retencionCompraV2DaoService.selectById(id, NombreEntidadesCompra.RETENCION_COMPRA_V2);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("RetencionCompraV2 ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getByCriteria") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	public Response getByCriteria(List<DatosBusqueda> datos) {
		try {
			List<RetencionCompraV2> lista = retencionCompraV2DaoService.selectByCriteria(datos, NombreEntidadesCompra.RETENCION_COMPRA_V2);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(RetencionCompraV2 registro) {
		System.out.println("LLEGA AL SERVICIO PUT RetencionCompraV2");
		try {
			RetencionCompraV2 resultado = retencionCompraV2Service.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar RetencionCompraV2: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(RetencionCompraV2 registro) {
		System.out.println("LLEGA AL SERVICIO POST RetencionCompraV2");
		try {
			RetencionCompraV2 resultado = retencionCompraV2Service.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear RetencionCompraV2: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			retencionCompraV2Service.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("RetencionCompraV2 eliminada correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
