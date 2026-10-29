package com.saa.ws.rest.cnt;

import java.util.HashMap;
import java.util.Map;

import com.saa.ejb.cnt.estadofinanciero.EncabezadoEstadoFinanciero;
import com.saa.ejb.cnt.estadofinanciero.SolicitudEstadoFinanciero;
import com.saa.ejb.cnt.service.EstadoFinancieroOficialService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Estados financieros oficiales (C19). Ver docs/logica-negocio/rpr/ESTADOS-FINANCIEROS-OFICIALES.md.
 * <ul>
 * <li>POST /esfo/encabezado: razon social, RUC, nombre del estado, periodo, PRELIMINAR/DEFINITIVO, firmas y
 * nombre de archivo sugerido (para el CSV del frontend).</li>
 * <li>POST /esfo/datos: lo anterior + filas con resultado del ejercicio y comparativo (JSON).</li>
 * <li>POST /esfo/imprimir: PDF (por defecto) o EXCEL con logo, encabezado y firmas; el nombre del archivo es el
 * del estado y su periodo (Content-Disposition).</li>
 * </ul>
 */
@Path("esfo")
public class EstadoFinancieroOficialRest {

	@EJB
	private EstadoFinancieroOficialService estadoFinancieroOficialService;

	@POST
	@Path("encabezado")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response encabezado(SolicitudEstadoFinanciero s) {
		try {
			return Response.ok(estadoFinancieroOficialService.encabezado(s)).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	@POST
	@Path("datos")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response datos(SolicitudEstadoFinanciero s) {
		try {
			return Response.ok(estadoFinancieroOficialService.datos(s)).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	@POST
	@Path("imprimir")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ "application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", MediaType.APPLICATION_JSON })
	public Response imprimir(SolicitudEstadoFinanciero s) {
		try {
			EncabezadoEstadoFinanciero e = estadoFinancieroOficialService.encabezado(s);
			byte[] archivo = estadoFinancieroOficialService.imprimir(s);
			boolean pdf = s.getFormato() == null || "PDF".equalsIgnoreCase(s.getFormato().trim());
			String nombre = e.getNombreArchivo() + (pdf ? ".pdf" : ".xlsx");
			return Response.ok(archivo)
					.header("Content-Disposition", "attachment; filename=\"" + nombre + "\"")
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.type(pdf ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
					.build();
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
