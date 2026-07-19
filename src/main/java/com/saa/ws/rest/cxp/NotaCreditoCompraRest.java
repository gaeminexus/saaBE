package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.ejb.cxp.service.NotaCreditoCompraService;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("ntcc")
public class NotaCreditoCompraRest {
	@EJB private NotaCreditoCompraDaoService notaCreditoCompraDaoService;
	@EJB private NotaCreditoCompraService notaCreditoCompraService;
	@Context private UriInfo context;
	public NotaCreditoCompraRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<NotaCreditoCompra> lista = notaCreditoCompraDaoService.selectAll(NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			NotaCreditoCompra entidad = notaCreditoCompraDaoService.selectById(id, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("NotaCreditoCompra ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getByCriteria") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	public Response getByCriteria(List<DatosBusqueda> datos) {
		try {
			List<NotaCreditoCompra> lista = notaCreditoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(NotaCreditoCompra registro) {
		System.out.println("LLEGA AL SERVICIO PUT NotaCreditoCompra");
		try {
			NotaCreditoCompra resultado = notaCreditoCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar NotaCreditoCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(NotaCreditoCompra registro) {
		System.out.println("LLEGA AL SERVICIO POST NotaCreditoCompra");
		try {
			NotaCreditoCompra resultado = notaCreditoCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear NotaCreditoCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			notaCreditoCompraService.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("NotaCreditoCompra eliminada correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
