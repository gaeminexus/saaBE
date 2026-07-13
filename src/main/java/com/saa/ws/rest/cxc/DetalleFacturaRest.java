package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.DetalleFacturaDaoService;
import com.saa.ejb.cxc.service.DetalleFacturaService;
import com.saa.model.cxc.DetalleFactura;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("dtfc")
public class DetalleFacturaRest {

	@EJB
	private DetalleFacturaDaoService detalleFacturaDaoService;

	@EJB
	private DetalleFacturaService detalleFacturaService;

	@Context
	private UriInfo context;

	public DetalleFacturaRest() {}

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<DetalleFactura> lista = detalleFacturaDaoService.selectAll(NombreEntidadesCobro.DETALLE_FACTURA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener DetalleFactura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			DetalleFactura entidad = detalleFacturaDaoService.selectById(id, NombreEntidadesCobro.DETALLE_FACTURA);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("DetalleFactura con ID " + id + " no encontrado")
						.type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener DetalleFactura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(DetalleFactura registro) {
		System.out.println("LLEGA AL SERVICIO PUT DetalleFactura");
		try {
			DetalleFactura resultado = detalleFacturaService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar DetalleFactura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(DetalleFactura registro) {
		System.out.println("LLEGA AL SERVICIO POST DetalleFactura");
		try {
			DetalleFactura resultado = detalleFacturaService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al crear DetalleFactura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE DetalleFactura con id: " + id);
		try {
			List<Long> ids = new java.util.ArrayList<>();
			ids.add(id);
			detalleFacturaService.remove(ids);
			return Response.status(Response.Status.OK)
					.entity("DetalleFactura eliminado correctamente")
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al eliminar DetalleFactura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("LLEGA AL SERVICIO selectByCriteria DetalleFactura");
		try {
			List<DetalleFactura> result = detalleFacturaService.selectByCriteria(datos);
			return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error en selectByCriteria DetalleFactura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}
}
