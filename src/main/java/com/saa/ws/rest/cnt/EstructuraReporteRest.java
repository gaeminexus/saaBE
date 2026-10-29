package com.saa.ws.rest.cnt;

import java.util.HashMap;
import java.util.Map;

import com.saa.ejb.cnt.estructura.ArchivoEstructura;
import com.saa.ejb.cnt.estructura.ResultadoEstructura;
import com.saa.ejb.cnt.estructura.SolicitudEstructura;
import com.saa.ejb.cnt.service.EstructuraReporteService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Estructuras de renglones (C19): B17 y demas reportes parametrizados en CNT.RNRP / CNT.CRRP.
 * <ul>
 * <li>POST /esrp/generar: lineas, suma de control, advertencias y el contenido del archivo (JSON).</li>
 * <li>POST /esrp/archivo: el archivo de la estructura tal como se presenta (B17M3968ddMMaaaa.txt, latin1, CRLF).</li>
 * </ul>
 * Cuerpo: {@code {empresa, codigoEstructura | codigoAlterno, fechaCorte, fechaInicio?, fechaComparacion?}}.
 * Ver docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md.
 */
@Path("esrp")
public class EstructuraReporteRest {

	@EJB
	private EstructuraReporteService estructuraReporteService;

	@POST
	@Path("generar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response generar(SolicitudEstructura solicitud) {
		try {
			ResultadoEstructura r = estructuraReporteService.generar(solicitud);
			return Response.ok(r).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	@POST
	@Path("archivo")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
	public Response archivo(SolicitudEstructura solicitud) {
		try {
			ResultadoEstructura r = estructuraReporteService.generar(solicitud);
			if (r.getContenidoArchivo() == null) {
				Map<String, Object> m = new HashMap<>();
				m.put("mensaje", "La estructura " + r.getNombre() + " no tiene formato de archivo (RPRTFRAR)");
				return Response.status(Response.Status.BAD_REQUEST).entity(m).type(MediaType.APPLICATION_JSON).build();
			}
			return Response.ok(ArchivoEstructura.bytes(r.getContenidoArchivo()))
					.header("Content-Disposition", "attachment; filename=\"" + r.getNombreArchivo() + "\"")
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.type("text/plain; charset=ISO-8859-1").build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	private Response error(Throwable e) {
		Map<String, Object> m = new HashMap<>();
		m.put("mensaje", e.getMessage());
		return Response.status(Response.Status.BAD_REQUEST).entity(m).type(MediaType.APPLICATION_JSON).build();
	}
}
