package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.DetalleLiquidacionCompraDaoService;
import com.saa.ejb.cxc.service.DetalleLiquidacionCompraService;
import com.saa.model.cxc.DetalleLiquidacionCompra;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("dtlc")
public class DetalleLiquidacionCompraRest {

	@EJB
	private DetalleLiquidacionCompraDaoService detalleLiquidacionCompraDaoService;

	@EJB
	private DetalleLiquidacionCompraService detalleLiquidacionCompraService;

	public DetalleLiquidacionCompraRest() {}

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<DetalleLiquidacionCompra> lista = detalleLiquidacionCompraDaoService.selectAll(NombreEntidadesCobro.DETALLE_LIQUIDACION_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener DetalleLiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			DetalleLiquidacionCompra entidad = detalleLiquidacionCompraDaoService.selectById(id, NombreEntidadesCobro.DETALLE_LIQUIDACION_COMPRA);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("DetalleLiquidacionCompra con ID " + id + " no encontrado")
						.type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener DetalleLiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(DetalleLiquidacionCompra registro) {
		try {
			DetalleLiquidacionCompra resultado = detalleLiquidacionCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar DetalleLiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(DetalleLiquidacionCompra registro) {
		try {
			DetalleLiquidacionCompra resultado = detalleLiquidacionCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al crear DetalleLiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			List<Long> ids = new java.util.ArrayList<>();
			ids.add(id);
			detalleLiquidacionCompraService.remove(ids);
			return Response.status(Response.Status.OK)
					.entity("DetalleLiquidacionCompra eliminado correctamente")
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al eliminar DetalleLiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		try {
			List<DetalleLiquidacionCompra> result = detalleLiquidacionCompraService.selectByCriteria(datos);
			return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error en selectByCriteria DetalleLiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}
}
