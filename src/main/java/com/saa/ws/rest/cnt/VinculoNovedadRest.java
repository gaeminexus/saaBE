package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.VinculoNovedadDaoService;
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
 * C25 — Vínculos de las novedades (CNT.VNNV): lecturas estándar, vincular y anular (un vínculo no se borra).
 *
 * <p>Las escrituras pasan por {@link SeguimientoNovedadService} (estados, usuario obligatorio U-2, historial);
 * no hay PUT ni DELETE físico: un registro de seguimiento no se edita a mano.</p>
 */
@Path("vnnv")
public class VinculoNovedadRest {

	@EJB
	private VinculoNovedadDaoService vinculoNovedadDaoService;

	@EJB
	private SeguimientoNovedadService servicio;

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		System.out.println("LLEGA AL SERVICIO GETALL - VINCULO NOVEDAD");
		try {
			return RespuestasC25.ok(vinculoNovedadDaoService.selectAll(NombreEntidadesContabilidad.VINCULO_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al obtener los registros: ");
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GETID - VINCULO NOVEDAD " + id);
		try {
			return RespuestasC25.ok(vinculoNovedadDaoService.selectById(id, NombreEntidadesContabilidad.VINCULO_NOVEDAD));
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
		System.out.println("selectByCriteria de VINCULO NOVEDAD");
		try {
			return RespuestasC25.ok(vinculoNovedadDaoService.selectByCriteria(registros, NombreEntidadesContabilidad.VINCULO_NOVEDAD));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error en la búsqueda: ");
		}
	}

	/** POST /vnnv — {usuario, idNovedad, tipoVinculo, ...} (igual que POST /nvdd/{id}/vincular). */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registrar(AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST vnnv/");
		try {
			return RespuestasC25.ok(servicio.vincular(accion.getIdNovedad(), accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al vincular: ");
		}
	}

	/** POST /vnnv/{id}/anular — {usuario, motivo}. */
	@POST
	@Path("/{id}/anular")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response anular(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST vnnv/{id}/anular");
		try {
			return RespuestasC25.ok(servicio.desvincular(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al anular el vínculo: ");
		}
	}

	/** DELETE /vnnv/{id}?usuario&motivo — anula el vínculo (no lo borra). */
	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response anularVinculo(@PathParam("id") Long id, @QueryParam("usuario") String usuario, @QueryParam("motivo") String motivo) {
		System.out.println("LLEGA AL SERVICIO DELETE vnnv/{id}");
		try {
			AccionNovedad accion = new AccionNovedad();
			accion.setUsuario(usuario);
			accion.setMotivo(motivo);
			return RespuestasC25.ok(servicio.desvincular(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al anular el vínculo: ");
		}
	}
}
