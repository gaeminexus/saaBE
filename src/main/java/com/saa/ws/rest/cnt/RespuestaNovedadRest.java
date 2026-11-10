package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.RespuestaNovedadDaoService;
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
 * C25 — Respuestas de ASOPREP (CNT.RSNV): lecturas estándar y revisión (aceptar / observar, por otro usuario).
 *
 * <p>Las escrituras pasan por {@link SeguimientoNovedadService} (estados, usuario obligatorio U-2, historial);
 * no hay PUT ni DELETE físico: un registro de seguimiento no se edita a mano.</p>
 */
@Path("rsnv")
public class RespuestaNovedadRest {

	@EJB
	private RespuestaNovedadDaoService respuestaNovedadDaoService;

	@EJB
	private SeguimientoNovedadService servicio;

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		System.out.println("LLEGA AL SERVICIO GETALL - RESPUESTA NOVEDAD");
		try {
			return RespuestasC25.ok(respuestaNovedadDaoService.selectAll(NombreEntidadesContabilidad.RESPUESTA_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al obtener los registros: ");
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GETID - RESPUESTA NOVEDAD " + id);
		try {
			return RespuestasC25.ok(respuestaNovedadDaoService.selectById(id, NombreEntidadesContabilidad.RESPUESTA_NOVEDAD));
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
		System.out.println("selectByCriteria de RESPUESTA NOVEDAD");
		try {
			return RespuestasC25.ok(respuestaNovedadDaoService.selectByCriteria(registros, NombreEntidadesContabilidad.RESPUESTA_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error en la búsqueda: ");
		}
	}

	/** POST /rsnv/{id}/aceptar — {usuario, motivo?}. */
	@POST
	@Path("/{id}/aceptar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response aceptar(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST rsnv/{id}/aceptar");
		try {
			return RespuestasC25.ok(servicio.aceptar(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al aceptar la respuesta: ");
		}
	}

	/** POST /rsnv/{id}/observar — {usuario, motivo}. */
	@POST
	@Path("/{id}/observar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response observar(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST rsnv/{id}/observar");
		try {
			return RespuestasC25.ok(servicio.observar(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al observar la respuesta: ");
		}
	}
}
