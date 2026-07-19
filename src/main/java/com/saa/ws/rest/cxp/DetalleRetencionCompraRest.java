package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.DetalleRetencionCompraDaoService;
import com.saa.ejb.cxp.service.DetalleRetencionCompraService;
import com.saa.model.cxp.DetalleRetencionCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("drcm")
public class DetalleRetencionCompraRest {
	@EJB private DetalleRetencionCompraDaoService detalleRetencionCompraDaoService;
	@EJB private DetalleRetencionCompraService detalleRetencionCompraService;
	@Context private UriInfo context;
	public DetalleRetencionCompraRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<DetalleRetencionCompra> lista = detalleRetencionCompraDaoService.selectAll(NombreEntidadesCompra.DETALLE_RETENCION_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			DetalleRetencionCompra entidad = detalleRetencionCompraDaoService.selectById(id, NombreEntidadesCompra.DETALLE_RETENCION_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("DetalleRetencionCompra ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getByCriteria") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	public Response getByCriteria(List<DatosBusqueda> datos) {
		try {
			List<DetalleRetencionCompra> lista = detalleRetencionCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.DETALLE_RETENCION_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(DetalleRetencionCompra registro) {
		try {
			DetalleRetencionCompra resultado = detalleRetencionCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(DetalleRetencionCompra registro) {
		try {
			DetalleRetencionCompra resultado = detalleRetencionCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			detalleRetencionCompraService.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("DetalleRetencionCompra eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
