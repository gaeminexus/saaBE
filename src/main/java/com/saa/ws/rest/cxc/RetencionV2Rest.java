package com.saa.ws.rest.cxc;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.RetencionV2DaoService;
import com.saa.ejb.cxc.service.RetencionV2Service;
import com.saa.model.cxc.RetencionV2;
import com.saa.model.cxc.NombreEntidadesCobro;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("rtv2")
public class RetencionV2Rest {
	@EJB
	private RetencionV2DaoService retencionV2DaoService;
	@EJB
	private RetencionV2Service retencionV2Service;
	@Context
	private UriInfo context;
	public RetencionV2Rest() {}
	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<RetencionV2> lista = retencionV2DaoService.selectAll(NombreEntidadesCobro.RETENCION_V2);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			RetencionV2 entidad = retencionV2DaoService.selectById(id, NombreEntidadesCobro.RETENCION_V2);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND).entity("RetencionV2 ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(RetencionV2 registro) {
		System.out.println("LLEGA AL SERVICIO PUT RetencionV2");
		try {
			RetencionV2 resultado = retencionV2Service.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(RetencionV2 registro) {
		System.out.println("LLEGA AL SERVICIO POST RetencionV2");
		try {
			RetencionV2 resultado = retencionV2Service.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE RetencionV2 con id: " + id);
		try {
			List<Long> ids = new java.util.ArrayList<>();
			ids.add(id);
			retencionV2Service.remove(ids);
			return Response.status(Response.Status.OK).entity("RetencionV2 eliminada correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("LLEGA AL SERVICIO selectByCriteria RetencionV2");
		try {
			List<RetencionV2> result = retencionV2Service.selectByCriteria(datos);
			return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	
	/**
	 * ENDPOINT PRINCIPAL: Procesa un comprobante de retención V2 completo.
	 * Ejecuta todo el flujo: valida, graba, genera XML v2.0.0, firma y autoriza ante el SRI.
	 * Si el facturador tiene generaConta=1, también genera el asiento contable.
	 *
	 * Body JSON esperado:
	 * <pre>
	 * {
	 *   "retencion": {
	 *     "facturador": { "id": 1 },
	 *     "proveedor":  { "codigo": 10 },
	 *     "ptoEmision": { "id": 1 },
	 *     "numEstablecimiento": "001",
	 *     "numPtoEmision": "001",
	 *     "periodoFiscal": "06/2026",
	 *     "fecha": "2026-07-23T00:00:00",
	 *     "observacion": "...",
	 *     "detalleRetencionV2": [
	 *       {
	 *         "tipoDocReten":           "01",
	 *         "numDocReten":            "001-001-000000123",
	 *         "fechaEmiDoc":            "2026-07-20",
	 *         "docResAutorizacion":     "2606202301...",
	 *         "docResTotalSinImpuestos": 1000.00,
	 *         "docResIvaCero":           0.00,
	 *         "docResPorIva":            15.00,
	 *         "docResTotalIva":          150.00,
	 *         "docResTotal":             1150.00,
	 *         "docResForPago":           "01",
	 *         "codImpuesto":             "1",
	 *         "codRetencion":            "303",
	 *         "baseImponible":           1000.00,
	 *         "porcentajeReten":         1.00,
	 *         "valorReten":              10.00
	 *       }
	 *     ]
	 *   }
	 * }
	 * </pre>
	 *
	 * Configuración automática (forzada en fase de pruebas):
	 * - ambiente:     1 (PRUEBA → celcer.sri.gob.ec). Cambiar a 2L para producción.
	 * - conectaSRI:   1 (SI)
	 * - destinatario: se obtiene del email del proveedor
	 * - pathLogo:     resources/logos/logo_aso.png
	 *
	 * @param params Map con la clave "retencion"
	 * @return JSON con el resultado completo del proceso
	 */
	@POST
	@Path("/procesarCompleta")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response procesarRetencionV2Completa(java.util.Map<String, Object> params) {
		try {
			@SuppressWarnings("unchecked")
			java.util.Map<String, Object> retencionMap =
					(java.util.Map<String, Object>) params.get("retencion");

			if (retencionMap == null) {
				java.util.Map<String, Object> err = new java.util.HashMap<>();
				err.put("exito", false);
				err.put("etapa", "PARAMETROS");
				err.put("mensaje", "El parámetro 'retencion' es obligatorio.");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(err).type(MediaType.APPLICATION_JSON).build();
			}

			// ── Extraer detalleRetencionV2 del mapa (dentro del objeto retencion) ─
			@SuppressWarnings("unchecked")
			java.util.List<java.util.Map<String, Object>> detallesMap =
					(java.util.List<java.util.Map<String, Object>>) retencionMap.get("detalleRetencionV2");

			if (detallesMap == null || detallesMap.isEmpty()) {
				java.util.Map<String, Object> err = new java.util.HashMap<>();
				err.put("exito", false);
				err.put("etapa", "PARAMETROS");
				err.put("mensaje", "La retención V2 debe tener al menos un detalle (detalleRetencionV2).");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(err).type(MediaType.APPLICATION_JSON).build();
			}

			// ── Convertir cabecera y detalles ─────────────────────────────────
			com.fasterxml.jackson.databind.ObjectMapper mapper = createObjectMapper();
			RetencionV2 retencion = mapper.convertValue(retencionMap, RetencionV2.class);

			java.util.List<com.saa.model.cxc.DetalleRetencionV2> detalles = new java.util.ArrayList<>();
			for (java.util.Map<String, Object> dMap : detallesMap) {
				com.saa.model.cxc.DetalleRetencionV2 det =
						mapper.convertValue(dMap, com.saa.model.cxc.DetalleRetencionV2.class);
				detalles.add(det);
			}

			// ── Llamar al servicio ────────────────────────────────────────────
			java.util.Map<String, Object> resultado = retencionV2Service.procesarRetencionV2Completa(
					retencion, detalles,
					1L,   // ambiente PRUEBA — cambiar a 2L para producción
					1L,   // conectaSRI = SI
					null, // destinatario: se toma del email del proveedor
					null  // pathLogo: default
			);

			// ── Determinar código HTTP según resultado ────────────────────────
			boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
			String etapa  = (String) resultado.getOrDefault("etapa", "");

			if (exito) {
				return Response.status(Response.Status.OK)
						.entity(resultado).type(MediaType.APPLICATION_JSON).build();
			} else if ("VALIDACION_CONTABLE".equals(etapa) || "VALIDACION_FACTURA".equals(etapa)
					|| "PARAMETROS".equals(etapa)) {
				// Error de datos del usuario, nada se grabó todavía → 422
				return Response.status(422)
						.entity(resultado).type(MediaType.APPLICATION_JSON).build();
			} else if ("AUTORIZACION_SRI".equals(etapa)) {
				// SRI rechazó por lógica (NO_AUTORIZADO): registro queda en BD → 200 con exito=false
				return Response.status(Response.Status.OK)
						.entity(resultado).type(MediaType.APPLICATION_JSON).build();
			} else if ("XML_DEVUELTO".equals(etapa)) {
				// SRI rechazó por formato XML (DEVUELTA): el registro se eliminó
				// porque el comprobante nunca quedó en el SRI → 422
				return Response.status(422)
						.entity(resultado).type(MediaType.APPLICATION_JSON).build();
			} else {
				// ERROR_AUTORIZACION_SRI, GRABADO_RETENCION, GENERACION_XML, ERROR_INESPERADO
				// → error técnico → 500. Ojo: en ERROR_AUTORIZACION_SRI la retención
				// SÍ queda grabada (el XML pudo llegar al SRI); el mensaje indica que
				// se debe consultar el estado para completar el proceso.
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(resultado).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Throwable e) {
			System.err.println("ERROR en procesarRetencionV2Completa REST: " + e.getMessage());
			e.printStackTrace();
			java.util.Map<String, Object> err = new java.util.HashMap<>();
			err.put("exito", false);
			err.put("etapa", "ERROR_INESPERADO");
			err.put("mensaje", "Error inesperado en el servidor: " + e.getMessage());
			err.put("error", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(err).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Genera el XML de retención V2 electrónica según estándares del SRI v2.0.0
	 * @param clave Clave de acceso de la retención
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return JSON con el resultado de la generación
	 */
	@GET
	@Path("/generarXML/{clave}/{ambiente}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response generarXML(@PathParam("clave") String clave, @PathParam("ambiente") Long ambiente) {
		System.out.println("LLEGA AL SERVICIO generarXML RetencionV2 con clave: " + clave + " y ambiente: " + ambiente);
		try {
			String[] resultado = retencionV2Service.generarXMLRetencionV2(clave, ambiente);
			
			// Crear objeto de respuesta con la información
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
	 * Autoriza la retención V2 electrónica ante el SRI
	 * @param params Mapa con los parámetros de autorización
	 * @return JSON con el resultado de la autorización
	 */
	@POST
	@Path("/autorizar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response autorizarRetencionV2(java.util.Map<String, Object> params) {
		System.out.println("LLEGA AL SERVICIO autorizarRetencionV2");
		try {
			// Extraer parámetros del JSON
			Long idFacturador = getLongParam(params, "idFacturador");
			Long ambiente = getLongParam(params, "ambiente");
			Long conectaSRI = getLongParam(params, "conectaSRI");
			String clave = (String) params.get("clave");
			Long codigoRetencion = getLongParam(params, "idRetencion");
			String xml = (String) params.get("xml");
			String destinatario = (String) params.get("destinatario");
			String pathLogo = (String) params.get("pathLogo");
			
			// Validar parámetros obligatorios
			if (idFacturador == null || ambiente == null || conectaSRI == null || 
					clave == null || codigoRetencion == null || xml == null) {
				java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
				errorResponse.put("mensaje", "ERROR");
				errorResponse.put("error", "Faltan parámetros obligatorios");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}
			
			java.util.Map<String, Object> resultado = retencionV2Service.autorizarRetencionV2(idFacturador, ambiente, conectaSRI, 
					clave, codigoRetencion, xml, destinatario, pathLogo);
			
			java.util.Map<String, Object> response = new java.util.HashMap<>();
			response.put("mensaje", resultado.get("mensaje"));
			response.put("clave", clave);
			// Opcionalmente indicar si se generó el PDF
			if (resultado.get("pdfBytes") != null) {
				response.put("pdfGenerado", true);
			}
			
			return Response.status(Response.Status.OK).entity(response).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			e.printStackTrace();
			java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
			errorResponse.put("mensaje", "ERROR");
			errorResponse.put("error", e.getMessage());
			
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponse)
					.type(MediaType.APPLICATION_JSON).build();
		}
	}
	
	private Long getLongParam(java.util.Map<String, Object> params, String key) {
		Object value = params.get(key);
		if (value == null) return null;
		if (value instanceof Long) return (Long) value;
		if (value instanceof Integer) return ((Integer) value).longValue();
		if (value instanceof String) return Long.parseLong((String) value);
		return null;
	}

	private com.fasterxml.jackson.databind.ObjectMapper createObjectMapper() {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
		mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}

	/**
	 * Anula una retención V2 y su asiento contable vinculado.
	 * Body JSON: { "idRetencion": 123, "motivo": "...", "usuario": "..." }
	 */
	@POST
	@Path("/anular")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response anularRetencionV2(java.util.Map<String, Object> params) {
		try {
			Object idObj = params.get("idRetencion");
			if (idObj == null) {
				java.util.Map<String, Object> err = new java.util.HashMap<>();
				err.put("exito", false);
				err.put("mensaje", "El parámetro 'idRetencion' es obligatorio.");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(err).type(MediaType.APPLICATION_JSON).build();
			}
			Long idRetencion = null;
			if (idObj instanceof Integer) idRetencion = ((Integer) idObj).longValue();
			else if (idObj instanceof Long) idRetencion = (Long) idObj;
			else idRetencion = Long.parseLong(idObj.toString());
			String motivo  = params.get("motivo")  != null ? params.get("motivo").toString()  : null;
			String usuario = params.get("usuario") != null ? params.get("usuario").toString() : null;
			Object idUsuarioObj = params.get("idUsuario");
			Long idUsuario = null;
			if (idUsuarioObj instanceof Integer) idUsuario = ((Integer) idUsuarioObj).longValue();
			else if (idUsuarioObj instanceof Long) idUsuario = (Long) idUsuarioObj;
			else if (idUsuarioObj != null) idUsuario = Long.parseLong(idUsuarioObj.toString());
			boolean anularEnCascada = Boolean.TRUE.equals(params.get("anularEnCascada"));

			java.util.Map<String, Object> resultado = retencionV2Service.anularRetencionV2(
					idRetencion, motivo, usuario, idUsuario, anularEnCascada);

			boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
			return Response.status(exito ? Response.Status.OK : Response.Status.BAD_REQUEST)
					.entity(resultado).type(MediaType.APPLICATION_JSON).build();

		} catch (com.saa.basico.util.IncomeException e) {
			java.util.Map<String, Object> err = new java.util.HashMap<>();
			err.put("exito", false);
			err.put("mensaje", e.getMessage());
			return Response.status(Response.Status.CONFLICT).entity(err).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			System.err.println("ERROR en anularRetencionV2 REST: " + e.getMessage());
			e.printStackTrace();
			java.util.Map<String, Object> err = new java.util.HashMap<>();
			err.put("exito", false);
			err.put("mensaje", "Error inesperado al anular la retención V2: " + e.getMessage());
			err.put("error", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(err).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/movimientosRelacionados/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response movimientosRelacionadosRetencionV2(@PathParam("id") Long idRetencion) {
		try {
			return Response.ok(retencionV2Service.movimientosRelacionadosRetencionV2(idRetencion))
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			java.util.Map<String, Object> err = new java.util.HashMap<>();
			err.put("exito", false);
			err.put("mensaje", "Error al consultar movimientos relacionados: " + e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(err).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Reenvía el email de una retención V2 autorizada.
	 * Si el PDF no existe en disco lo regenera al vuelo (sirve para documentos anteriores al fix).
	 * Body JSON: { "idRetencion": 123, "destinatarios": "a@x.com;b@x.com" }
	 */
	@POST
	@Path("/reenviarEmail")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response reenviarEmail(java.util.Map<String, Object> params) {
		System.out.println("LLEGA AL SERVICIO reenviarEmail RTV2");
		try {
			Long id = null;
			Object idObj = params.get("idRetencion");
			if (idObj instanceof Integer) id = ((Integer) idObj).longValue();
			else if (idObj instanceof Long) id = (Long) idObj;
			else if (idObj != null) id = Long.parseLong(idObj.toString());

			String destinatarios = (String) params.get("destinatarios");
			if (id == null) {
				java.util.Map<String, Object> err = new java.util.HashMap<>();
				err.put("exito", false);
				err.put("mensaje", "El parámetro 'idRetencion' es obligatorio.");
				return Response.status(Response.Status.BAD_REQUEST).entity(err).type(MediaType.APPLICATION_JSON).build();
			}
			java.util.Map<String, Object> resultado = retencionV2Service.reenviarEmail(id, destinatarios);
			boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
			return Response.status(exito ? Response.Status.OK : Response.Status.BAD_REQUEST)
					.entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			System.err.println("ERROR en reenviarEmail RTV2 REST: " + e.getMessage());
			java.util.Map<String, Object> err = new java.util.HashMap<>();
			err.put("exito", false);
			err.put("mensaje", "Error inesperado al reenviar el email: " + e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(err).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Consulta el estado de una retención V2 ante el SRI y, si devuelve AUTORIZADO:
	 *  - Actualiza el estado de la retención a autorizada (5) si estaba pendiente.
	 *  - Guarda el número de autorización y fecha de autorización.
	 *  - Si la retención no tiene asiento contable y el facturador tiene generaConta=1,
	 *    genera el asiento contable automáticamente.
	 *  - Envía el email con el XML autorizado y PDF RIDE adjuntos.
	 *
	 * POST /rtv2/consultarYActualizarEstado
	 * Body: { "idRetencion": 123 }
	 */
	@POST
	@Path("/consultarYActualizarEstado")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response consultarYActualizarEstado(java.util.Map<String, Object> params) {
		System.out.println("LLEGA AL SERVICIO consultarYActualizarEstado RETENCION V2");
		try {
			Long idRetencion = getLongParam(params, "idRetencion");
			if (idRetencion == null) {
				java.util.Map<String, Object> err = new java.util.HashMap<>();
				err.put("exito", false);
				err.put("mensaje", "El parámetro 'idRetencion' es obligatorio.");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(err).type(MediaType.APPLICATION_JSON).build();
			}

			java.util.Map<String, Object> resultado =
					retencionV2Service.consultarYActualizarEstadoRetencionV2(idRetencion);

			boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
			return Response.status(exito ? Response.Status.OK : Response.Status.BAD_REQUEST)
					.entity(resultado).type(MediaType.APPLICATION_JSON).build();

		} catch (Throwable e) {
			System.err.println("ERROR en consultarYActualizarEstado RTV2 REST: " + e.getMessage());
			e.printStackTrace();
			java.util.Map<String, Object> err = new java.util.HashMap<>();
			err.put("exito", false);
			err.put("mensaje", "Error inesperado al consultar estado en el SRI: " + e.getMessage());
			err.put("error", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(err).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
