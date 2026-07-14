package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.FacturaDaoService;
import com.saa.ejb.cxc.service.FacturaService;
import com.saa.model.cxc.Factura;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("fctr")
public class FacturaRest {

	@EJB
	private FacturaDaoService facturaDaoService;

	@EJB
	private FacturaService facturaService;

	@Context
	private UriInfo context;

	public FacturaRest() {}

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<Factura> lista = facturaDaoService.selectAll(NombreEntidadesCobro.FACTURA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener Factura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			Factura entidad = facturaDaoService.selectById(id, NombreEntidadesCobro.FACTURA);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("Factura con ID " + id + " no encontrada")
						.type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener Factura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(Factura registro) {
		System.out.println("LLEGA AL SERVICIO PUT Factura");
		try {
			Factura resultado = facturaService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar Factura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(Factura registro) {
		System.out.println("LLEGA AL SERVICIO POST Factura");
		try {
			Factura resultado = facturaService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al crear Factura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * ENDPOINT PRINCIPAL: Procesa una factura completa.
	 * Graba en BD, genera XML, firma electrónicamente y autoriza ante el SRI.
	 * AMBIENTE FORZADO A 1 (PRUEBAS → celcer.sri.gob.ec) durante fase de pruebas.
	 * Para producción cambiar 1L por 2L.
	 */
	@POST
	@Path("/procesarCompleta")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response procesarFacturaCompleta(java.util.Map<String, Object> params) {
		System.out.println("=== LLEGA AL SERVICIO procesarFacturaCompleta ===");
		try {
			@SuppressWarnings("unchecked")
			java.util.Map<String, Object> facturaMap = (java.util.Map<String, Object>) params.get("factura");

			if (facturaMap == null) {
				java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
				errorResponse.put("exito", false);
				errorResponse.put("etapa", "PARAMETROS");
				errorResponse.put("mensaje", "El parámetro 'factura' es obligatorio.");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}

			@SuppressWarnings("unchecked")
			java.util.List<java.util.Map<String, Object>> detallesMap =
				(java.util.List<java.util.Map<String, Object>>) facturaMap.get("detalleFactura");

			if (detallesMap == null || detallesMap.isEmpty()) {
				java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
				errorResponse.put("exito", false);
				errorResponse.put("etapa", "PARAMETROS");
				errorResponse.put("mensaje", "La factura debe tener al menos un detalle (producto).");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}

			Factura factura = convertMapToFactura(facturaMap);

			java.util.List<com.saa.model.cxc.DetalleFactura> detalles = new java.util.ArrayList<>();
			com.fasterxml.jackson.databind.ObjectMapper mapper = createObjectMapper();
			for (java.util.Map<String, Object> detalleMap : detallesMap) {
				com.saa.model.cxc.DetalleFactura detalle =
					mapper.convertValue(detalleMap, com.saa.model.cxc.DetalleFactura.class);
				detalles.add(detalle);
			}

			java.util.Map<String, Object> resultado = facturaService.procesarFacturaCompleta(
				factura, detalles, 1L, 1L, null, null);

			// Determinar el código HTTP según el resultado
			boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
			String etapa  = (String) resultado.getOrDefault("etapa", "");

			if (exito) {
				return Response.status(Response.Status.OK)
						.entity(resultado)
						.type(MediaType.APPLICATION_JSON).build();
			} else if ("VALIDACION_CONTABLE".equals(etapa) || "PARAMETROS".equals(etapa)) {
				// Error de validación → 422 Unprocessable Entity
				return Response.status(422)
						.entity(resultado)
						.type(MediaType.APPLICATION_JSON).build();
			} else if ("AUTORIZACION_SRI".equals(etapa)) {
				// SRI rechazó → 200 con exito=false (el documento existe pero no fue autorizado)
				return Response.status(Response.Status.OK)
						.entity(resultado)
						.type(MediaType.APPLICATION_JSON).build();
			} else {
				// Otro error de proceso → 500
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(resultado)
						.type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Throwable e) {
			System.err.println("ERROR en procesarFacturaCompleta REST: " + e.getMessage());
			e.printStackTrace();
			java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
			errorResponse.put("exito", false);
			errorResponse.put("etapa", "ERROR_INESPERADO");
			errorResponse.put("mensaje", "Error inesperado en el servidor: " + e.getMessage());
			errorResponse.put("error", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponse)
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE Factura con id: " + id);
		try {
			List<Long> ids = new java.util.ArrayList<>();
			ids.add(id);
			facturaService.remove(ids);
			return Response.status(Response.Status.OK)
					.entity("Factura eliminada correctamente")
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al eliminar Factura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("LLEGA AL SERVICIO selectByCriteria Factura");
		try {
			List<Factura> result = facturaService.selectByCriteria(datos);
			return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error en selectByCriteria Factura: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Genera el XML de factura electrónica según estándares del SRI v1.1.0
	 */
	@GET
	@Path("/generarXML/{clave}/{ambiente}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response generarXML(@PathParam("clave") String clave, @PathParam("ambiente") Long ambiente) {
		System.out.println("LLEGA AL SERVICIO generarXML con clave: " + clave + " y ambiente: " + ambiente);
		try {
			String[] resultado = facturaService.generarXMLFactura(clave, ambiente);
			java.util.Map<String, String> response = new java.util.HashMap<>();
			response.put("mensaje", resultado[0]);
			response.put("pathRelativo", resultado[1]);
			response.put("pathAbsoluto", resultado[2]);
			response.put("clave", clave);
			response.put("ambiente", String.valueOf(ambiente));
			return Response.status(Response.Status.OK).entity(response).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			e.printStackTrace();
			java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
			errorResponse.put("mensaje", "ERROR");
			errorResponse.put("error", e.getMessage());
			errorResponse.put("clave", clave);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponse)
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Autoriza la factura electrónica ante el SRI.
	 * Igual que el PHP de referencia:
	 *   ambiente=1 → celcer.sri.gob.ec (pruebas)
	 *   ambiente=2 → cel.sri.gob.ec (producción)
	 */
	@POST
	@Path("/autorizar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response autorizarFactura(java.util.Map<String, Object> params) {
		System.out.println("LLEGA AL SERVICIO autorizarFactura");
		try {
			Long idFacturador = getLongParam(params, "idFacturador");
			Long ambiente     = getLongParam(params, "ambiente");
			Long conectaSRI   = getLongParam(params, "conectaSRI");
			String clave      = (String) params.get("clave");
			Long codigoFactura= getLongParam(params, "codigoFactura");
			Double subsidio   = getDoubleParam(params, "subsidio");
			String xml        = (String) params.get("xml");
			String destinatario = (String) params.get("destinatario");
			String pathLogo   = (String) params.get("pathLogo");

			if (idFacturador == null || ambiente == null || conectaSRI == null
					|| clave == null || codigoFactura == null || xml == null) {
				java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
				errorResponse.put("mensaje", "ERROR");
				errorResponse.put("error", "Faltan parámetros: idFacturador, ambiente, conectaSRI, clave, codigoFactura, xml");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}

			String resultado = facturaService.autorizarFactura(
					idFacturador, ambiente, conectaSRI, clave,
					codigoFactura, subsidio, xml, destinatario, pathLogo);

			java.util.Map<String, String> response = new java.util.HashMap<>();
			response.put("mensaje", "OK");
			response.put("resultado", resultado);
			response.put("clave", clave);
			response.put("idFacturador", String.valueOf(idFacturador));
			response.put("ambiente", String.valueOf(ambiente));
			return Response.status(Response.Status.OK).entity(response).type(MediaType.APPLICATION_JSON).build();

		} catch (Throwable e) {
			e.printStackTrace();
			java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
			errorResponse.put("mensaje", "ERROR");
			errorResponse.put("error", e.getMessage());
			errorResponse.put("detalle", e.getClass().getName());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponse)
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	// ── MÉTODOS AUXILIARES ─────────────────────────────────────

	private Factura convertMapToFactura(java.util.Map<String, Object> map) {
		return createObjectMapper().convertValue(map, Factura.class);
	}

	private com.fasterxml.jackson.databind.ObjectMapper createObjectMapper() {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
		mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// Zona horaria de Ecuador (UTC-5) para evitar que fechas enviadas como
		// timestamp (medianoche UTC) se conviertan al día siguiente
		mapper.setTimeZone(java.util.TimeZone.getTimeZone("America/Guayaquil"));
		return mapper;
	}

	private Long getLongParam(java.util.Map<String, Object> params, String key) {
		Object value = params.get(key);
		if (value == null) return null;
		if (value instanceof Long) return (Long) value;
		if (value instanceof Integer) return ((Integer) value).longValue();
		if (value instanceof String) return Long.parseLong((String) value);
		return null;
	}

	private Double getDoubleParam(java.util.Map<String, Object> params, String key) {
		Object value = params.get(key);
		if (value == null) return null;
		if (value instanceof Double) return (Double) value;
		if (value instanceof java.math.BigDecimal) return ((java.math.BigDecimal) value).doubleValue();
		if (value instanceof Integer) return ((Integer) value).doubleValue();
		if (value instanceof Long) return ((Long) value).doubleValue();
		if (value instanceof String) return Double.parseDouble((String) value);
		return null;
	}
}