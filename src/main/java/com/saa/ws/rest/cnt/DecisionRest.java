package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.DecisionDaoService;
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
 * C25 — Decisiones (CNT.DCSN): lecturas estándar, registro con constancia por Contabilidad de ASOPREP (D-056 (4)) y
 * texto para DECISIONES-Y-PARTIDAS.md (D-056 (3)).
 *
 * <p>Las escrituras pasan por {@link SeguimientoNovedadService} (estados, usuario obligatorio U-2, historial);
 * no hay PUT ni DELETE físico: un registro de seguimiento no se edita a mano.</p>
 */
@Path("dcsn")
public class DecisionRest {

	@EJB
	private DecisionDaoService decisionDaoService;

	@EJB
	private SeguimientoNovedadService servicio;

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		System.out.println("LLEGA AL SERVICIO GETALL - DECISION");
		try {
			return RespuestasC25.ok(decisionDaoService.selectAll(NombreEntidadesContabilidad.DECISION));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al obtener los registros: ");
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GETID - DECISION " + id);
		try {
			return RespuestasC25.ok(decisionDaoService.selectById(id, NombreEntidadesContabilidad.DECISION));
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
		System.out.println("selectByCriteria de DECISION");
		try {
			return RespuestasC25.ok(decisionDaoService.selectByCriteria(registros, NombreEntidadesContabilidad.DECISION));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error en la búsqueda: ");
		}
	}

	/** POST /dcsn/registrar?idEmpresa — {usuario, tema, texto, fecha?, numero?, tipoDecide?, cargo?, evidencia?, idRespuesta?|idRequerimiento?|idNovedad?, adjuntos[] (obligatorio)}. */
	@POST
	@Path("/registrar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registrar(@QueryParam("idEmpresa") Long idEmpresa, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST dcsn/registrar");
		try {
			return RespuestasC25.ok(servicio.registrarDecision(idEmpresa, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al registrar la decisión: ");
		}
	}

	/** POST /dcsn/{id}/copiada — {usuario}: ya está en DECISIONES-Y-PARTIDAS.md. */
	@POST
	@Path("/{id}/copiada")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response copiada(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST dcsn/{id}/copiada");
		try {
			return RespuestasC25.ok(servicio.marcarDecisionCopiada(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al marcar la decisión: ");
		}
	}

	/** GET /dcsn/{id}/textoExpediente — fila lista para pegar en DECISIONES-Y-PARTIDAS.md. */
	@GET
	@Path("/{id}/textoExpediente")
	@Produces(MediaType.APPLICATION_JSON)
	public Response textoExpediente(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GET dcsn/{id}/textoExpediente");
		try {
			return RespuestasC25.ok(java.util.Collections.singletonMap("texto", servicio.textoExpediente(id)));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al armar el texto: ");
		}
	}

	/** GET /dcsn/listado?idEmpresa&texto — C25b: decisiones tomadas con su ficha (tema, texto, quién decide, evidencia, razones) y las novedades vinculadas. */
	@GET
	@Path("/listado")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listado(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("texto") String texto) {
		System.out.println("LLEGA AL SERVICIO GET dcsn/listado");
		try {
			return RespuestasC25.ok(servicio.decisiones(idEmpresa, texto));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al consultar las decisiones: ");
		}
	}
}
