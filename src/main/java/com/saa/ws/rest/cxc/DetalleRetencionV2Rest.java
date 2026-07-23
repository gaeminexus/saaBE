package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.DetalleRetencionV2DaoService;
import com.saa.ejb.cxc.service.DetalleRetencionV2Service;
import com.saa.model.cxc.DetalleRetencionV2;
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

@Path("drv2")
public class DetalleRetencionV2Rest {

	@EJB
	private DetalleRetencionV2DaoService detalleRetencionV2DaoService;

	@EJB
	private DetalleRetencionV2Service detalleRetencionV2Service;

	@Context
	private UriInfo context;

	public DetalleRetencionV2Rest() {}

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<DetalleRetencionV2> lista =
					detalleRetencionV2DaoService.selectAll(NombreEntidadesCobro.DETALLE_RETENCION_V2);
			return Response.status(Response.Status.OK).entity(lista)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener DetalleRetencionV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			DetalleRetencionV2 entidad =
					detalleRetencionV2DaoService.selectById(id, NombreEntidadesCobro.DETALLE_RETENCION_V2);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("DetalleRetencionV2 con ID " + id + " no encontrado")
						.type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener DetalleRetencionV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * GET - Recupera todos los detalles de una RetenciónV2 por su ID.
	 */
	@GET
	@Path("/getByRetencionV2/{idRetencionV2}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getByRetencionV2(@PathParam("idRetencionV2") Long idRetencionV2) {
		try {
			List<DetalleRetencionV2> lista =
					detalleRetencionV2Service.selectByRetencionV2(idRetencionV2);
			return Response.status(Response.Status.OK).entity(lista)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener detalles de RetencionV2 " + idRetencionV2 + ": " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(DetalleRetencionV2 registro) {
		System.out.println("LLEGA AL SERVICIO PUT DetalleRetencionV2");
		try {
			DetalleRetencionV2 resultado = detalleRetencionV2Service.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar DetalleRetencionV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(DetalleRetencionV2 registro) {
		System.out.println("LLEGA AL SERVICIO POST DetalleRetencionV2");
		try {
			DetalleRetencionV2 resultado = detalleRetencionV2Service.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al crear DetalleRetencionV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("LLEGA AL SERVICIO selectByCriteria DetalleRetencionV2");
		try {
			List<DetalleRetencionV2> result = detalleRetencionV2Service.selectByCriteria(datos);
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
		System.out.println("LLEGA AL SERVICIO DELETE DetalleRetencionV2 con id: " + id);
		try {
			DetalleRetencionV2 elimina = new DetalleRetencionV2();
			detalleRetencionV2DaoService.remove(elimina, id);
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al eliminar DetalleRetencionV2: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}
}
