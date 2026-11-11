package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.NovedadSeguimientoDaoService;
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
 * C25 — Seguimiento de novedades: novedades del mes (CNT.NVDD). Lecturas estándar, bandeja, detalle, resumen,
 * carga del lote del expediente y transiciones de la novedad. Contrato completo y pruebas curl: docs/logica-negocio/cnt/C25-SEGUIMIENTO-NOVEDADES.md.
 *
 * <p>Las escrituras pasan por {@link SeguimientoNovedadService} (estados, usuario obligatorio U-2, historial);
 * no hay PUT ni DELETE físico: un registro de seguimiento no se edita a mano.</p>
 */
@Path("nvdd")
public class NovedadSeguimientoRest {

	@EJB
	private NovedadSeguimientoDaoService novedadSeguimientoDaoService;

	@EJB
	private SeguimientoNovedadService servicio;

	@EJB
	private com.saa.ejb.cnt.service.FichaReferenciaService fichaReferenciaService;

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		System.out.println("LLEGA AL SERVICIO GETALL - NOVEDAD");
		try {
			return RespuestasC25.ok(novedadSeguimientoDaoService.selectAll(NombreEntidadesContabilidad.NOVEDAD_SEGUIMIENTO));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al obtener los registros: ");
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GETID - NOVEDAD " + id);
		try {
			return RespuestasC25.ok(novedadSeguimientoDaoService.selectById(id, NombreEntidadesContabilidad.NOVEDAD_SEGUIMIENTO));
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
		System.out.println("selectByCriteria de NOVEDAD");
		try {
			return RespuestasC25.ok(novedadSeguimientoDaoService.selectByCriteria(registros, NombreEntidadesContabilidad.NOVEDAD_SEGUIMIENTO));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error en la búsqueda: ");
		}
	}

	/** GET /nvdd/bandeja?idEmpresa&anio&mes&modulo&tipo&estado&area&texto&incluirReemplazadas — una fila por novedad (sin CLOB). */
	@GET
	@Path("/bandeja")
	@Produces(MediaType.APPLICATION_JSON)
	public Response bandeja(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes, @QueryParam("modulo") String modulo, @QueryParam("tipo") String tipo, @QueryParam("estado") Long estado, @QueryParam("area") String area, @QueryParam("texto") String texto, @QueryParam("incluirReemplazadas") boolean incluirReemplazadas) {
		System.out.println("LLEGA AL SERVICIO GET nvdd/bandeja");
		try {
			return RespuestasC25.ok(servicio.bandeja(idEmpresa, anio, mes, modulo, tipo, estado, area, texto, incluirReemplazadas));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al consultar la bandeja de novedades: ");
		}
	}

	/** GET /nvdd/vistaRequerimientos?... — una fila por requerimiento (lista de trabajo de cada área). */
	@GET
	@Path("/vistaRequerimientos")
	@Produces(MediaType.APPLICATION_JSON)
	public Response vistaRequerimientos(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes, @QueryParam("modulo") String modulo, @QueryParam("tipo") String tipo, @QueryParam("estado") Long estado, @QueryParam("area") String area, @QueryParam("texto") String texto) {
		System.out.println("LLEGA AL SERVICIO GET nvdd/vistaRequerimientos");
		try {
			return RespuestasC25.ok(servicio.vistaRequerimientos(idEmpresa, anio, mes, modulo, tipo, estado, area, texto));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al consultar los requerimientos: ");
		}
	}

	/** GET /nvdd/{id}/detalle — novedad, requerimientos, respuestas, adjuntos, vínculos (asientos con líneas), cuentas por regularizar e historial. */
	@GET
	@Path("/{id}/detalle")
	@Produces(MediaType.APPLICATION_JSON)
	public Response detalle(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GET nvdd/{id}/detalle");
		try {
			return RespuestasC25.ok(servicio.detalle(id));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al consultar el detalle de la novedad: ");
		}
	}

	/** GET /nvdd/{id}/historial */
	@GET
	@Path("/{id}/historial")
	@Produces(MediaType.APPLICATION_JSON)
	public Response historial(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GET nvdd/{id}/historial");
		try {
			return RespuestasC25.ok(servicio.historial(id));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al consultar el historial: ");
		}
	}

	/** GET /nvdd/resumenMes?idEmpresa&anio&mes — contadores, antigüedad y cuentas por regularizar. */
	@GET
	@Path("/resumenMes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resumenMes(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes) {
		System.out.println("LLEGA AL SERVICIO GET nvdd/resumenMes");
		try {
			return RespuestasC25.ok(servicio.resumenMes(idEmpresa, anio, mes));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al consultar el resumen del mes: ");
		}
	}

	/** GET /nvdd/cuentasPorRegularizar?idEmpresa&anio&mes — saldo en vivo desde CNT.DTAS (preparado para C22). */
	@GET
	@Path("/cuentasPorRegularizar")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cuentasPorRegularizar(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes) {
		System.out.println("LLEGA AL SERVICIO GET nvdd/cuentasPorRegularizar");
		try {
			return RespuestasC25.ok(servicio.cuentasPorRegularizar(idEmpresa, anio, mes));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al consultar las cuentas por regularizar: ");
		}
	}

	/** GET /nvdd/reporteActa?idEmpresa&anio&mes — datos y texto (markdown) del anexo del acta mensual. */
	@GET
	@Path("/reporteActa")
	@Produces(MediaType.APPLICATION_JSON)
	public Response reporteActa(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes) {
		System.out.println("LLEGA AL SERVICIO GET nvdd/reporteActa");
		try {
			return RespuestasC25.ok(servicio.reporteActa(idEmpresa, anio, mes));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al armar el reporte para el acta: ");
		}
	}

	/** GET /nvdd/pendientePorArea?idEmpresa&anio&mes&area — pedido pendiente por área (texto para enviar). */
	@GET
	@Path("/pendientePorArea")
	@Produces(MediaType.APPLICATION_JSON)
	public Response pendientePorArea(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes, @QueryParam("area") String area) {
		System.out.println("LLEGA AL SERVICIO GET nvdd/pendientePorArea");
		try {
			return RespuestasC25.ok(servicio.pendientePorArea(idEmpresa, anio, mes, area));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al armar el pedido pendiente: ");
		}
	}

	/** POST /nvdd/cargaLote?idEmpresa&usuario&simular=true|false — cuerpo: novedades-AAAA-MM.json del expediente. */
	@POST
	@Path("/cargaLote")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cargaLote(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("usuario") String usuario, @QueryParam("simular") boolean simular, com.saa.ejb.cnt.service.dto.LoteNovedades lote) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/cargaLote");
		try {
			return RespuestasC25.ok(servicio.cargarLote(idEmpresa, usuario, simular, lote));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al cargar el lote de novedades: ");
		}
	}

	/** POST /nvdd/cargaDecisiones?idEmpresa&usuario&simular — cuerpo: resultados/decisiones.json del expediente. */
	@POST
	@Path("/cargaDecisiones")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cargaDecisiones(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("usuario") String usuario, @QueryParam("simular") boolean simular, com.saa.ejb.cnt.service.dto.LoteNovedades.LoteDecisiones lote) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/cargaDecisiones");
		try {
			return RespuestasC25.ok(servicio.cargarDecisiones(idEmpresa, usuario, simular, lote));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al cargar las decisiones: ");
		}
	}

	/** POST /nvdd/enviar?idEmpresa&anio&mes — {usuario, ids[], fechaLimite}: BORRADOR -> ENVIADA (sin ids, todo el mes). */
	@POST
	@Path("/enviar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response enviar(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/enviar");
		try {
			return RespuestasC25.ok(servicio.enviar(idEmpresa, anio, mes, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al enviar las novedades: ");
		}
	}

	/** POST /nvdd/cerrarInformativas?idEmpresa&anio&mes — {usuario, motivo}: cierra en bloque las INFORMATIVA enviadas. */
	@POST
	@Path("/cerrarInformativas")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cerrarInformativas(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/cerrarInformativas");
		try {
			return RespuestasC25.ok(servicio.cerrarInformativas(idEmpresa, anio, mes, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al cerrar las informativas: ");
		}
	}

	/** POST /nvdd/{id}/cerrar — {usuario, motivo}. */
	@POST
	@Path("/{id}/cerrar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cerrar(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/{id}/cerrar");
		try {
			return RespuestasC25.ok(servicio.cerrar(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al cerrar la novedad: ");
		}
	}

	/** POST /nvdd/{id}/reabrir — {usuario, motivo}. */
	@POST
	@Path("/{id}/reabrir")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response reabrir(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/{id}/reabrir");
		try {
			return RespuestasC25.ok(servicio.reabrir(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al reabrir la novedad: ");
		}
	}

	/** POST /nvdd/{id}/anular — {usuario, motivo}. */
	@POST
	@Path("/{id}/anular")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response anular(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/{id}/anular");
		try {
			return RespuestasC25.ok(servicio.anular(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al anular la novedad: ");
		}
	}

	/** POST /nvdd/{id}/nuevaVersion — {usuario, motivo}. */
	@POST
	@Path("/{id}/nuevaVersion")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response nuevaVersion(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/{id}/nuevaVersion");
		try {
			return RespuestasC25.ok(servicio.nuevaVersion(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al crear la versión nueva: ");
		}
	}

	/** POST /nvdd/{id}/ajuste — {usuario, asientos:["DIA-2026-11-0005", "15321"], idRequerimiento?, motivo?, valor?}: asientos manuales de Contabilidad validados contra CNT.ASNT (D-056 (7)). */
	@POST
	@Path("/{id}/ajuste")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response ajuste(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/{id}/ajuste");
		try {
			return RespuestasC25.ok(servicio.registrarAjuste(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al registrar los asientos de ajuste: ");
		}
	}

	/** POST /nvdd/{id}/vincular — {usuario, tipoVinculo, idAsiento|asientos|idDecision|numero|idNovedadRelacionada|idPartidaC22|tipoDocumentoModulo+idDocumentoModulo}. */
	@POST
	@Path("/{id}/vincular")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response vincular(@PathParam("id") Long id, AccionNovedad accion) {
		System.out.println("LLEGA AL SERVICIO POST nvdd/{id}/vincular");
		try {
			return RespuestasC25.ok(servicio.vincular(id, accion));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al vincular: ");
		}
	}

	/**
	 * GET /nvdd/referencia?idEmpresa&tipo&valor&anio&mes — C25b: ficha de solo lectura de un elemento citado en la
	 * pantalla (ASIENTO, PRESTAMO, OPERACION, PAGO, ENTIDAD, COBRO, FILA, CARGA, CORRIDA, CUENTA, CXP, CXC, EMPLEADO,
	 * EJECUCION, DECISION, NOVEDAD). Trae la ruta de la pantalla del módulo cuando existe una que abra el registro.
	 */
	@GET
	@Path("/referencia")
	@Produces(MediaType.APPLICATION_JSON)
	public Response referencia(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("tipo") String tipo, @QueryParam("valor") String valor,
			@QueryParam("anio") Long anio, @QueryParam("mes") Long mes) {
		System.out.println("LLEGA AL SERVICIO GET nvdd/referencia " + tipo + " " + valor);
		try {
			return RespuestasC25.ok(fichaReferenciaService.ficha(idEmpresa, tipo, valor, anio, mes));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "Error al consultar la referencia: ");
		}
	}
}
