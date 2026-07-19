package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.FormaPagoLiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.service.FormaPagoLiquidacionCompraCompraService;
import com.saa.model.cxp.FormaPagoLiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("fplm")
public class FormaPagoLiquidacionCompraCompraRest {
	@EJB private FormaPagoLiquidacionCompraCompraDaoService formaPagoLiquidacionCompraCompraDaoService;
	@EJB private FormaPagoLiquidacionCompraCompraService formaPagoLiquidacionCompraCompraService;
	@Context private UriInfo context;
	public FormaPagoLiquidacionCompraCompraRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<FormaPagoLiquidacionCompraCompra> lista = formaPagoLiquidacionCompraCompraDaoService.selectAll(NombreEntidadesCompra.FORMA_PAGO_LIQUIDACION_COMPRA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			FormaPagoLiquidacionCompraCompra entidad = formaPagoLiquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.FORMA_PAGO_LIQUIDACION_COMPRA_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("FormaPagoLiquidacionCompraCompra ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getByCriteria") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	public Response getByCriteria(List<DatosBusqueda> datos) {
		try {
			List<FormaPagoLiquidacionCompraCompra> lista = formaPagoLiquidacionCompraCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.FORMA_PAGO_LIQUIDACION_COMPRA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(FormaPagoLiquidacionCompraCompra registro) {
		try {
			FormaPagoLiquidacionCompraCompra resultado = formaPagoLiquidacionCompraCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(FormaPagoLiquidacionCompraCompra registro) {
		try {
			FormaPagoLiquidacionCompraCompra resultado = formaPagoLiquidacionCompraCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			formaPagoLiquidacionCompraCompraService.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("FormaPagoLiquidacionCompraCompra eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
