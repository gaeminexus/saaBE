package com.saa.ws.rest.cnt;

import com.saa.ejb.cnt.service.SeguimientoNovedadV2Service;
import com.saa.ejb.cnt.service.dto.AccionNovedadV2;
import com.saa.ejb.cnt.service.dto.HechoCreditoDto;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * C34 — Seguimiento de Novedades v2 (/nvdd2). Bandeja por grupo (por regularizar, pedidos, informativas), detalle
 * explicado para el usuario, justificación de líneas con reparto a cuentas, respuesta de pedidos, cierre, dar por
 * conocidas y exportación para el re-proceso v8. Contrato y pruebas curl: docs/logica-negocio/cnt/C34-NOVEDADES-V2.md.
 *
 * <p>Errores de negocio: 400 con el mensaje para el usuario; lo inesperado, 500 (RespuestasC25).</p>
 */
@Path("nvdd2")
public class NovedadV2Rest {

	@EJB
	private SeguimientoNovedadV2Service servicio;

	@GET
	@Path("/resumen")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resumen(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/resumen");
		try {
			return RespuestasC25.ok(servicio.resumen(idEmpresa, anio, mes));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo leer el resumen: ");
		}
	}

	@GET
	@Path("/bandeja")
	@Produces(MediaType.APPLICATION_JSON)
	public Response bandeja(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes,
			@QueryParam("grupo") String grupo, @QueryParam("area") String area, @QueryParam("estado") String estado, @QueryParam("texto") String texto) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/bandeja");
		try {
			return RespuestasC25.ok(servicio.bandeja(idEmpresa, anio, mes, grupo, area, estado, texto));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo leer la bandeja: ");
		}
	}

	@GET
	@Path("/{id}/fila")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fila(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/{id}/fila");
		try {
			return RespuestasC25.ok(servicio.fila(id));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo leer la novedad: ");
		}
	}

	@GET
	@Path("/{id}/detalle")
	@Produces(MediaType.APPLICATION_JSON)
	public Response detalle(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/{id}/detalle");
		try {
			return RespuestasC25.ok(servicio.detalle(id));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo leer el detalle de la novedad: ");
		}
	}

	@GET
	@Path("/buscar/decisiones")
	@Produces(MediaType.APPLICATION_JSON)
	public Response buscarDecisiones(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("idNovedad") Long idNovedad, @QueryParam("texto") String texto) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/buscar/decisiones");
		try {
			return RespuestasC25.ok(servicio.buscarDecisiones(idEmpresa, idNovedad, texto));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudieron buscar las decisiones: ");
		}
	}

	@GET
	@Path("/buscar/cuentas")
	@Produces(MediaType.APPLICATION_JSON)
	public Response buscarCuentas(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("texto") String texto) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/buscar/cuentas");
		try {
			return RespuestasC25.ok(servicio.buscarCuentas(idEmpresa, texto));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudieron buscar las cuentas: ");
		}
	}

	@GET
	@Path("/buscar/prestamos")
	@Produces(MediaType.APPLICATION_JSON)
	public Response buscarPrestamos(@QueryParam("texto") String texto) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/buscar/prestamos");
		try {
			return RespuestasC25.ok(servicio.buscarPrestamos(texto));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudieron buscar los préstamos: ");
		}
	}

	@GET
	@Path("/buscar/participes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response buscarParticipes(@QueryParam("texto") String texto) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/buscar/participes");
		try {
			return RespuestasC25.ok(servicio.buscarParticipes(texto));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudieron buscar los partícipes: ");
		}
	}

	@GET
	@Path("/prestamo/{id}/cuotas")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cuotas(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/prestamo/{id}/cuotas");
		try {
			return RespuestasC25.ok(servicio.cuotasPrestamo(id));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudieron leer las cuotas: ");
		}
	}

	@POST
	@Path("/justificar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response justificar(AccionNovedadV2 a) {
		System.out.println("LLEGA AL SERVICIO POST nvdd2/justificar");
		try {
			return RespuestasC25.ok(servicio.justificar(a));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo justificar: ");
		}
	}

	@POST
	@Path("/justificaciones/{id}/anular")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response anularJustificacion(@PathParam("id") Long id, AccionNovedadV2 a) {
		System.out.println("LLEGA AL SERVICIO POST nvdd2/justificaciones/{id}/anular");
		try {
			return RespuestasC25.ok(servicio.anularJustificacion(id, a));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo anular la justificación: ");
		}
	}

	@POST
	@Path("/pedidos/{id}/responder")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response responder(@PathParam("id") Long id, AccionNovedadV2 a) {
		System.out.println("LLEGA AL SERVICIO POST nvdd2/pedidos/{id}/responder");
		try {
			return RespuestasC25.ok(servicio.responder(id, a));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo registrar la respuesta: ");
		}
	}

	@POST
	@Path("/respuestas/{id}/anular")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response anularRespuesta(@PathParam("id") Long id, AccionNovedadV2 a) {
		System.out.println("LLEGA AL SERVICIO POST nvdd2/respuestas/{id}/anular");
		try {
			return RespuestasC25.ok(servicio.anularRespuesta(id, a));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo anular la respuesta: ");
		}
	}

	@POST
	@Path("/{id}/cerrar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cerrar(@PathParam("id") Long id, AccionNovedadV2 a) {
		System.out.println("LLEGA AL SERVICIO POST nvdd2/{id}/cerrar");
		try {
			return RespuestasC25.ok(servicio.cerrar(id, a));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo cerrar: ");
		}
	}

	@POST
	@Path("/informativas/conocidas")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response conocidas(AccionNovedadV2 a) {
		System.out.println("LLEGA AL SERVICIO POST nvdd2/informativas/conocidas");
		try {
			return RespuestasC25.ok(servicio.darPorConocidas(a));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudieron dar por conocidas: ");
		}
	}

	@POST
	@Path("/{id}/reabrir")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response reabrir(@PathParam("id") Long id, AccionNovedadV2 a) {
		System.out.println("LLEGA AL SERVICIO POST nvdd2/{id}/reabrir");
		try {
			return RespuestasC25.ok(servicio.reabrir(id, a));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo reabrir: ");
		}
	}

	@GET
	@Path("/exportar")
	@Produces(MediaType.APPLICATION_JSON)
	public Response exportar(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("anio") Long anio, @QueryParam("mes") Long mes,
			@QueryParam("formato") String formato) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/exportar");
		try {
			return RespuestasC25.ok(servicio.exportar(idEmpresa, anio, mes, formato));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo exportar: ");
		}
	}

	// ---- C34d: hechos de crédito

	@GET
	@Path("/origenes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response origenes(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("fuente") String fuente, @QueryParam("idPrestamo") Long idPrestamo,
			@QueryParam("idEntidad") Long idEntidad, @QueryParam("texto") String texto, @QueryParam("desde") String desde,
			@QueryParam("hasta") String hasta, @QueryParam("valor") Double valor, @QueryParam("fecha") String fecha) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/origenes");
		try {
			return RespuestasC25.ok(servicio.buscarOrigenes(idEmpresa, fuente, idPrestamo, idEntidad, texto, desde, hasta, valor, fecha));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudieron buscar los orígenes: ");
		}
	}

	@GET
	@Path("/hechos/candidatos")
	@Produces(MediaType.APPLICATION_JSON)
	public Response candidatosHecho(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("idPrestamo") Long idPrestamo) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/hechos/candidatos");
		try {
			return RespuestasC25.ok(servicio.candidatosHecho(idEmpresa, idPrestamo));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudieron leer los movimientos de la operación: ");
		}
	}

	@POST
	@Path("/hechos")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registrarHecho(HechoCreditoDto h) {
		System.out.println("LLEGA AL SERVICIO POST nvdd2/hechos");
		try {
			return RespuestasC25.ok(servicio.registrarHecho(h));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo registrar el hecho de crédito: ");
		}
	}

	@POST
	@Path("/hechos/{id}/anular")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response anularHecho(@PathParam("id") Long id, HechoCreditoDto h) {
		System.out.println("LLEGA AL SERVICIO POST nvdd2/hechos/{id}/anular");
		try {
			return RespuestasC25.ok(servicio.anularHecho(id, h));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudo anular el hecho de crédito: ");
		}
	}

	@GET
	@Path("/exportar/hechos")
	@Produces(MediaType.APPLICATION_JSON)
	public Response exportarHechos(@QueryParam("idEmpresa") Long idEmpresa) {
		System.out.println("LLEGA AL SERVICIO GET nvdd2/exportar/hechos");
		try {
			return RespuestasC25.ok(servicio.exportarHechos(idEmpresa));
		} catch (Throwable e) {
			return RespuestasC25.error(e, "No se pudieron exportar los hechos: ");
		}
	}
}
