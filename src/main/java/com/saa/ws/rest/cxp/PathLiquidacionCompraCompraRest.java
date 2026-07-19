package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.PathLiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.service.PathLiquidacionCompraCompraService;
import com.saa.model.cxp.PathLiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("plcc")
public class PathLiquidacionCompraCompraRest {
	@EJB private PathLiquidacionCompraCompraDaoService pathLiquidacionCompraCompraDaoService;
	@EJB private PathLiquidacionCompraCompraService pathLiquidacionCompraCompraService;
	@Context private UriInfo context;
	public PathLiquidacionCompraCompraRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<PathLiquidacionCompraCompra> lista = pathLiquidacionCompraCompraDaoService.selectAll(NombreEntidadesCompra.PATH_LIQUIDACION_COMPRA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			PathLiquidacionCompraCompra entidad = pathLiquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.PATH_LIQUIDACION_COMPRA_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("PathLiquidacionCompraCompra ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getByCriteria") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
	public Response getByCriteria(List<DatosBusqueda> datos) {
		try {
			List<PathLiquidacionCompraCompra> lista = pathLiquidacionCompraCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.PATH_LIQUIDACION_COMPRA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(PathLiquidacionCompraCompra registro) {
		try {
			PathLiquidacionCompraCompra resultado = pathLiquidacionCompraCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(PathLiquidacionCompraCompra registro) {
		try {
			PathLiquidacionCompraCompra resultado = pathLiquidacionCompraCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			pathLiquidacionCompraCompraService.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("PathLiquidacionCompraCompra eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
