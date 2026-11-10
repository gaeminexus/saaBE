package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.HistorialNovedadDaoService;
import com.saa.ejb.cnt.service.SeguimientoNovedadService;
import com.saa.ejb.cnt.service.dto.AccionNovedad;
import com.saa.model.cnt.NombreEntidadesContabilidad;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * C25 — Historial de novedades (CNT.HSNV): solo lectura.
 *
 * <p>Las escrituras pasan por {@link SeguimientoNovedadService} (estados, usuario obligatorio U-2, historial);
 * no hay PUT ni DELETE físico: un registro de seguimiento no se edita a mano.</p>
 */
@Path("hsnv")
public class HistorialNovedadRest {

	@EJB
	private HistorialNovedadDaoService historialNovedadDaoService;

	@EJB
	private SeguimientoNovedadService servicio;

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		System.out.println("LLEGA AL SERVICIO GETALL - HISTORIAL NOVEDAD");
		try {
			return RespuestasC25.ok(historialNovedadDaoService.selectAll(NombreEntidadesContabilidad.HISTORIAL_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al obtener los registros: ");
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GETID - HISTORIAL NOVEDAD " + id);
		try {
			return RespuestasC25.ok(historialNovedadDaoService.selectById(id, NombreEntidadesContabilidad.HISTORIAL_NOVEDAD));
		} catch (jakarta.persistence.NoResultException e) {
			return Response.status(Response.Status.NOT_FOUND).entity("No existe el registro " + id + ".").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al obtener el registro: ");
		}
	}

	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> registros) {
		System.out.println("selectByCriteria de HISTORIAL NOVEDAD");
		try {
			return RespuestasC25.ok(historialNovedadDaoService.selectByCriteria(registros, NombreEntidadesContabilidad.HISTORIAL_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error en la búsqueda: ");
		}
	}
}
