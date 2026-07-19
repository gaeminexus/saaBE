package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.LiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.service.LiquidacionCompraCompraService;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("lqcc")
public class LiquidacionCompraCompraRest {
	@EJB private LiquidacionCompraCompraDaoService liquidacionCompraCompraDaoService;
	@EJB private LiquidacionCompraCompraService liquidacionCompraCompraService;
	@Context private UriInfo context;
	public LiquidacionCompraCompraRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<LiquidacionCompraCompra> lista = liquidacionCompraCompraDaoService.selectAll(NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener LiquidacionCompraCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			LiquidacionCompraCompra entidad = liquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("LiquidacionCompraCompra ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getByCriteria") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	public Response getByCriteria(List<DatosBusqueda> datos) {
		try {
			List<LiquidacionCompraCompra> lista = liquidacionCompraCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(LiquidacionCompraCompra registro) {
		System.out.println("LLEGA AL SERVICIO PUT LiquidacionCompraCompra");
		try {
			LiquidacionCompraCompra resultado = liquidacionCompraCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar LiquidacionCompraCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(LiquidacionCompraCompra registro) {
		System.out.println("LLEGA AL SERVICIO POST LiquidacionCompraCompra");
		try {
			LiquidacionCompraCompra resultado = liquidacionCompraCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear LiquidacionCompraCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			liquidacionCompraCompraService.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("LiquidacionCompraCompra eliminada correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
