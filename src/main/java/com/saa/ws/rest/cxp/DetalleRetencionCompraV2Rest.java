package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.DetalleRetencionCompraV2DaoService;
import com.saa.ejb.cxp.service.DetalleRetencionCompraV2Service;
import com.saa.model.cxp.DetalleRetencionCompraV2;
import com.saa.model.cxp.NombreEntidadesCompra;

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

@Path("drc2")
public class DetalleRetencionCompraV2Rest {

	@EJB
	private DetalleRetencionCompraV2DaoService detalleRetencionCompraV2DaoService;

	@EJB
	private DetalleRetencionCompraV2Service detalleRetencionCompraV2Service;

	@Context
	private UriInfo context;

	public DetalleRetencionCompraV2Rest() {}

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<DetalleRetencionCompraV2> lista =
					detalleRetencionCompraV2DaoService.selectAll(NombreEntidadesCompra.DETALLE_RETENCION_COMPRA_V2);
			return Response.status(Response.Status.OK).entity(lista)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener DetalleRetencionCompraV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			DetalleRetencionCompraV2 entidad =
					detalleRetencionCompraV2DaoService.selectById(id, NombreEntidadesCompra.DETALLE_RETENCION_COMPRA_V2);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("DetalleRetencionCompraV2 con ID " + id + " no encontrado")
						.type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener DetalleRetencionCompraV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * GET - Recupera todos los detalles de una Retención Compra V2 por su ID.
	 */
	@GET
	@Path("/getByRetencionCompraV2/{idRetencionCompraV2}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getByRetencionCompraV2(@PathParam("idRetencionCompraV2") Long idRetencionCompraV2) {
		try {
			List<DetalleRetencionCompraV2> lista =
					detalleRetencionCompraV2Service.selectByRetencionCompraV2(idRetencionCompraV2);
			return Response.status(Response.Status.OK).entity(lista)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener detalles de RetencionCompraV2 " + idRetencionCompraV2 + ": " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(DetalleRetencionCompraV2 registro) {
		System.out.println("LLEGA AL SERVICIO PUT DetalleRetencionCompraV2");
		try {
			DetalleRetencionCompraV2 resultado = detalleRetencionCompraV2Service.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar DetalleRetencionCompraV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(DetalleRetencionCompraV2 registro) {
		System.out.println("LLEGA AL SERVICIO POST DetalleRetencionCompraV2");
		try {
			DetalleRetencionCompraV2 resultado = detalleRetencionCompraV2Service.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al crear DetalleRetencionCompraV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("LLEGA AL SERVICIO selectByCriteria DetalleRetencionCompraV2");
		try {
			List<DetalleRetencionCompraV2> result =
					detalleRetencionCompraV2Service.selectByCriteria(datos);
			return Response.status(Response.Status.OK).entity(result)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE DetalleRetencionCompraV2 con id: " + id);
		try {
			DetalleRetencionCompraV2 elimina = new DetalleRetencionCompraV2();
			detalleRetencionCompraV2DaoService.remove(elimina, id);
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al eliminar DetalleRetencionCompraV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}
}
