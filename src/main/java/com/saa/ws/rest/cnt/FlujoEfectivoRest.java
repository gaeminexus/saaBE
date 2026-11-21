package com.saa.ws.rest.cnt;

import java.util.HashMap;
import java.util.Map;

import com.saa.ejb.cnt.flujo.ExcelFormatosAsoprep;
import com.saa.ejb.cnt.flujo.ResultadoComparativo;
import com.saa.ejb.cnt.flujo.ResultadoFlujoEfectivo;
import com.saa.ejb.cnt.flujo.SolicitudFlujoEfectivo;
import com.saa.ejb.cnt.service.FlujoEfectivoService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Formatos de ASOPREP derivados del B17 (C38). Ver docs/logica-negocio/rpr/FLUJO-EFECTIVO-Y-COMPARATIVO.md.
 * <ul>
 * <li>POST /flef/generar: estado de flujos de efectivo (indirecto) con lineas, subtotales y controles; con
 * {@code incluirDirecto: true}, tambien el flujo de caja directo (BIESS) mes a mes (JSON).</li>
 * <li>POST /flef/excel: el Excel del formato. {@code reporte}: INDIRECTO (por defecto, "FLUJO DE EFECTIVO") o
 * DIRECTO ("FLUJO DE CAJA" BIESS).</li>
 * <li>POST /flef/comparativo: balance comparativo con los codigos del B17 (JSON).</li>
 * <li>POST /flef/comparativoExcel: el Excel con el formato COMPARATIVO_01_2026_FORMATO.xlsx.</li>
 * </ul>
 * Cuerpo: {@code {empresa, anio, mes, periodo: MES|ACUMULADO, preliminar?, incluirDirecto?, reporte?}}. Sin
 * {@code preliminar: true} exige el mes cerrado (REPORTE_MES_NO_CERRADO), igual que el B17.
 */
@Path("flef")
public class FlujoEfectivoRest {

	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	@EJB
	private FlujoEfectivoService flujoEfectivoService;

	@POST
	@Path("generar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response generar(SolicitudFlujoEfectivo s) {
		try {
			return Response.ok(flujoEfectivoService.generar(s)).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	@POST
	@Path("excel")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ XLSX, MediaType.APPLICATION_JSON })
	public Response excel(SolicitudFlujoEfectivo s) {
		try {
			ResultadoFlujoEfectivo r = flujoEfectivoService.generar(s);
			boolean directo = s.esDirecto();
			byte[] archivo = directo ? ExcelFormatosAsoprep.flujoCaja(r) : ExcelFormatosAsoprep.flujoEfectivo(r);
			String nombre = (directo ? r.getNombreArchivo().replace("FLUJO_EFECTIVO_", "FLUJO_CAJA_") : r.getNombreArchivo()) + ".xlsx";
			return archivo(archivo, nombre);
		} catch (Throwable e) {
			return error(e);
		}
	}

	@POST
	@Path("comparativo")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response comparativo(SolicitudFlujoEfectivo s) {
		try {
			return Response.ok(flujoEfectivoService.comparativo(s)).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	@POST
	@Path("comparativoExcel")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ XLSX, MediaType.APPLICATION_JSON })
	public Response comparativoExcel(SolicitudFlujoEfectivo s) {
		try {
			ResultadoComparativo r = flujoEfectivoService.comparativo(s);
			return archivo(ExcelFormatosAsoprep.comparativo(r), r.getNombreArchivo() + ".xlsx");
		} catch (Throwable e) {
			return error(e);
		}
	}

	private Response archivo(byte[] archivo, String nombre) {
		return Response.ok(archivo)
				.header("Content-Disposition", "attachment; filename=\"" + nombre + "\"")
				.header("Access-Control-Expose-Headers", "Content-Disposition")
				.type(XLSX).build();
	}

	private Response error(Throwable e) {
		Map<String, Object> m = new HashMap<>();
		Throwable c = e;
		while (c.getCause() != null && (c.getMessage() == null || c instanceof jakarta.ejb.EJBException)) {
			c = c.getCause();
		}
		m.put("mensaje", c.getMessage() != null ? c.getMessage() : e.toString());
		return Response.status(Response.Status.BAD_REQUEST).entity(m).type(MediaType.APPLICATION_JSON).build();
	}
}
