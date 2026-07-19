package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.DetalleNotaDebitoCompraDaoService;
import com.saa.ejb.cxp.service.DetalleNotaDebitoCompraService;
import com.saa.model.cxp.DetalleNotaDebitoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("dtdc")
public class DetalleNotaDebitoCompraRest {
	@EJB private DetalleNotaDebitoCompraDaoService detalleNotaDebitoCompraDaoService;
	@EJB private DetalleNotaDebitoCompraService detalleNotaDebitoCompraService;
	@Context private UriInfo context;
	public DetalleNotaDebitoCompraRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<DetalleNotaDebitoCompra> lista = detalleNotaDebitoCompraDaoService.selectAll(NombreEntidadesCompra.DETALLE_NOTA_DEBITO_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			DetalleNotaDebitoCompra entidad = detalleNotaDebitoCompraDaoService.selectById(id, NombreEntidadesCompra.DETALLE_NOTA_DEBITO_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("DetalleNotaDebitoCompra ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getByCriteria") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	public Response getByCriteria(List<DatosBusqueda> datos) {
		try {
			List<DetalleNotaDebitoCompra> lista = detalleNotaDebitoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_NOTA_DEBITO_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(DetalleNotaDebitoCompra registro) {
		try {
			DetalleNotaDebitoCompra resultado = detalleNotaDebitoCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(DetalleNotaDebitoCompra registro) {
		try {
			DetalleNotaDebitoCompra resultado = detalleNotaDebitoCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			detalleNotaDebitoCompraService.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("DetalleNotaDebitoCompra eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
