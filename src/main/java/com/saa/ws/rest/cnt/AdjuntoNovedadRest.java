package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.AdjuntoNovedadDaoService;
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
 * C25 — Adjuntos (CNT.ADNV): el archivo se sube antes con POST /file/upload/custom (ruta novedades/AAAA-MM/...); aquí se registran sus metadatos.
 *
 * <p>Las escrituras pasan por {@link SeguimientoNovedadService} (estados, usuario obligatorio U-2, historial);
 * no hay PUT ni DELETE físico: un registro de seguimiento no se edita a mano.</p>
 */
@Path("adnv")
public class AdjuntoNovedadRest {

	@EJB
	private AdjuntoNovedadDaoService adjuntoNovedadDaoService;

	@EJB
	private SeguimientoNovedadService servicio;

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		System.out.println("LLEGA AL SERVICIO GETALL - ADJUNTO NOVEDAD");
		try {
			return RespuestasC25.ok(adjuntoNovedadDaoService.selectAll(NombreEntidadesContabilidad.ADJUNTO_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al obtener los registros: ");
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GETID - ADJUNTO NOVEDAD " + id);
		try {
			return RespuestasC25.ok(adjuntoNovedadDaoService.selectById(id, NombreEntidadesContabilidad.ADJUNTO_NOVEDAD));
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
		System.out.println("selectByCriteria de ADJUNTO NOVEDAD");
		try {
			return RespuestasC25.ok(adjuntoNovedadDaoService.selectByCriteria(registros, NombreEntidadesContabilidad.ADJUNTO_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error en la búsqueda: ");
		}
	}

	/** POST /adnv — {usuario, idNovedad|idRequerimiento|idRespuesta|idDecision, adjuntos[{tipoDocumento, nombreArchivo, url, mimeType, tamanio, hash}]}. */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registrar(AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST adnv/");
		try {
			return RespuestasC25.ok(servicio.registrarAdjunto(accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al registrar el adjunto: ");
		}
	}

	/** POST /adnv/{id}/copiadoExpediente — {usuario}: regla 2 del expediente. */
	@POST
	@Path("/{id}/copiadoExpediente")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response copiadoExpediente(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST adnv/{id}/copiadoExpediente");
		try {
			return RespuestasC25.ok(servicio.marcarAdjuntoCopiado(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al marcar el adjunto: ");
		}
	}
}
