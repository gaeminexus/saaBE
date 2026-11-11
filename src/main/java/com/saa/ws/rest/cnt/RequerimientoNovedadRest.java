package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.RequerimientoNovedadDaoService;
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
 * C25 — Requerimientos de las novedades (CNT.RQNV): lecturas estándar y respuesta de ASOPREP, no existe, no aplica y aplicación.
 *
 * <p>Las escrituras pasan por {@link SeguimientoNovedadService} (estados, usuario obligatorio U-2, historial);
 * no hay PUT ni DELETE físico: un registro de seguimiento no se edita a mano.</p>
 */
@Path("rqnv")
public class RequerimientoNovedadRest {

	@EJB
	private RequerimientoNovedadDaoService requerimientoNovedadDaoService;

	@EJB
	private SeguimientoNovedadService servicio;

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		System.out.println("LLEGA AL SERVICIO GETALL - REQUERIMIENTO NOVEDAD");
		try {
			return RespuestasC25.ok(requerimientoNovedadDaoService.selectAll(NombreEntidadesContabilidad.REQUERIMIENTO_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al obtener los registros: ");
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GETID - REQUERIMIENTO NOVEDAD " + id);
		try {
			return RespuestasC25.ok(requerimientoNovedadDaoService.selectById(id, NombreEntidadesContabilidad.REQUERIMIENTO_NOVEDAD));
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
		System.out.println("selectByCriteria de REQUERIMIENTO NOVEDAD");
		try {
			return RespuestasC25.ok(requerimientoNovedadDaoService.selectByCriteria(registros, NombreEntidadesContabilidad.REQUERIMIENTO_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error en la búsqueda: ");
		}
	}

	/** POST /rqnv/{id}/responder — {usuario, texto, opcion, canal, fecha, area, adjuntos[]}. */
	@POST
	@Path("/{id}/responder")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response responder(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST rqnv/{id}/responder");
		try {
			return RespuestasC25.ok(servicio.responder(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al registrar la respuesta: ");
		}
	}

	/** POST /rqnv/{id}/noExiste — {usuario, texto|motivo, canal}. */
	@POST
	@Path("/{id}/noExiste")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response noExiste(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST rqnv/{id}/noExiste");
		try {
			return RespuestasC25.ok(servicio.noExiste(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al registrar que el documento no existe: ");
		}
	}

	/** POST /rqnv/{id}/noAplica — {usuario, motivo}. */
	@POST
	@Path("/{id}/noAplica")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response noAplica(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST rqnv/{id}/noAplica");
		try {
			return RespuestasC25.ok(servicio.noAplica(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al marcar el requerimiento: ");
		}
	}

	/** POST /rqnv/{id}/aplicar — {usuario, asientos[] | sinAjusteMotivo}. */
	@POST
	@Path("/{id}/aplicar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response aplicar(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST rqnv/{id}/aplicar");
		try {
			return RespuestasC25.ok(servicio.aplicar(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al aplicar el requerimiento: ");
		}
	}
}
