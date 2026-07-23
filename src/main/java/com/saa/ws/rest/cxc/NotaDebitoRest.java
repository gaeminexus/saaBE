package com.saa.ws.rest.cxc;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.NotaDebitoDaoService;
import com.saa.ejb.cxc.service.NotaDebitoService;
import com.saa.model.cxc.NotaDebito;
import com.saa.model.cxc.NombreEntidadesCobro;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("ntdb")
public class NotaDebitoRest {
	@EJB
	private NotaDebitoDaoService notaDebitoDaoService;
	@EJB
	private NotaDebitoService notaDebitoService;
	@Context
	private UriInfo context;
	public NotaDebitoRest() {}
	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<NotaDebito> lista = notaDebitoDaoService.selectAll(NombreEntidadesCobro.NOTA_DEBITO);
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
			NotaDebito entidad = notaDebitoDaoService.selectById(id, NombreEntidadesCobro.NOTA_DEBITO);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND).entity("NotaDebito ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(NotaDebito registro) {
		System.out.println("LLEGA AL SERVICIO PUT NotaDebito");
		try {
			NotaDebito resultado = notaDebitoService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(NotaDebito registro) {
		System.out.println("LLEGA AL SERVICIO POST NotaDebito");
		try {
			NotaDebito resultado = notaDebitoService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	
	/**
	 * ENDPOINT PRINCIPAL: Procesa una nota de débito completa automáticamente.
	 * Este endpoint ejecuta todo el flujo: graba, genera XML, firma y autoriza ante el SRI.
	 * 
	 * El frontend solo debe enviar:
	 * {
	 *   "notaDebito": { objeto NotaDebito con todos los datos }
	 * }
	 * 
	 * Configuración automática:
	 * - ambiente: 1 (PRUEBA)
	 * - conectaSRI: 1 (SI)
	 * - destinatario: se obtiene del campo mail del comprador
	 * - pathLogo: resources/logos/logo_aso.png
	 * 
	 * @param params Mapa con el objeto notaDebito
	 * @return JSON con el resultado del proceso completo
	 */
	@POST
	@Path("/procesarCompleta")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response procesarNotaDebitoCompleta(java.util.Map<String, Object> params) {
		System.out.println("=== LLEGA AL SERVICIO procesarNotaDebitoCompleta ===");
		try {
			// Extraer parámetros del JSON
			@SuppressWarnings("unchecked")
			java.util.Map<String, Object> notaDebitoMap = (java.util.Map<String, Object>) params.get("notaDebito");
			
			// Validar parámetro obligatorio
			if (notaDebitoMap == null) {
				java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
				errorResponse.put("mensaje", "ERROR");
				errorResponse.put("error", "Parámetro 'notaDebito' es obligatorio");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}
			
			// Convertir el Map a objeto NotaDebito
			NotaDebito notaDebito = convertMapToNotaDebito(notaDebitoMap);

			// Extraer detalles del JSON si vienen en el request
			java.util.List<com.saa.model.cxc.DetalleNotaDebito> detalles = null;
			Object detObj = params.get("detalles");
			if (detObj instanceof java.util.List) {
				com.fasterxml.jackson.databind.ObjectMapper mapperDet = createObjectMapper();
				detalles = mapperDet.convertValue(detObj,
					mapperDet.getTypeFactory().constructCollectionType(
						java.util.List.class, com.saa.model.cxc.DetalleNotaDebito.class));
			}
			System.out.println("=== detalles recibidos en procesarCompleta: " + (detalles == null ? 0 : detalles.size()) + " ===");
			
			// Llamar al servicio que ejecuta todo el proceso
			java.util.Map<String, Object> resultado = notaDebitoService.procesarNotaDebitoCompleta(
				notaDebito,
				detalles, // detalles extraídos del JSON
				null,  // ambiente se configura automáticamente en el servicio
				null,  // conectaSRI se configura automáticamente en el servicio
				null,  // destinatario se obtiene del comprador
				null   // pathLogo se construye automáticamente
			);
			
			// Retornar resultado
			return Response.status(Response.Status.OK)
					.entity(resultado)
					.type(MediaType.APPLICATION_JSON).build();
					
		} catch (Throwable e) {
			System.err.println("ERROR en procesarNotaDebitoCompleta REST: " + e.getMessage());
			e.printStackTrace();
			
			java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
			errorResponse.put("mensaje", "ERROR");
			errorResponse.put("error", e.getMessage());
			errorResponse.put("exito", "false");
			
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponse)
					.type(MediaType.APPLICATION_JSON).build();
		}
	}
	
	/**
	 * Convierte un Map a objeto NotaDebito.
	 */
	private NotaDebito convertMapToNotaDebito(java.util.Map<String, Object> map) {
		com.fasterxml.jackson.databind.ObjectMapper mapper = createObjectMapper();
		return mapper.convertValue(map, NotaDebito.class);
	}
	
	private com.fasterxml.jackson.databind.ObjectMapper createObjectMapper() {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
		mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}
	
	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE NotaDebito con id: " + id);
		try {
			List<Long> ids = new java.util.ArrayList<>();
			ids.add(id);
			notaDebitoService.remove(ids);
			return Response.status(Response.Status.OK).entity("NotaDebito eliminada correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("LLEGA AL SERVICIO selectByCriteria NotaDebito");
		try {
			List<NotaDebito> result = notaDebitoService.selectByCriteria(datos);
			return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	
	/**
	 * Genera el XML de nota de débito electrónica según estándares del SRI v1.0.0
	 * @param clave Clave de acceso de la nota de débito
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return JSON con el resultado de la generación
	 */
	@GET
	@Path("/generarXML/{clave}/{ambiente}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response generarXML(@PathParam("clave") String clave, @PathParam("ambiente") Long ambiente) {
		System.out.println("LLEGA AL SERVICIO generarXML NotaDebito con clave: " + clave + " y ambiente: " + ambiente);
		try {
			String[] resultado = notaDebitoService.generarXMLNotaDebito(clave, ambiente);
			
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
	 * Autoriza la nota de débito electrónica ante el SRI
	 * @param params Mapa con los parámetros de autorización
	 * @return JSON con el resultado de la autorización
	 */
	@POST
	@Path("/autorizar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response autorizarNotaDebito(java.util.Map<String, Object> params) {
		System.out.println("LLEGA AL SERVICIO autorizarNotaDebito");
		try {
			Long idFacturador = getLongParam(params, "idFacturador");
			Long ambiente = getLongParam(params, "ambiente");
			Long conectaSRI = getLongParam(params, "conectaSRI");
			String clave = (String) params.get("clave");
			Long codigoNotaDebito = getLongParam(params, "idNotaDebito");
			String xml = (String) params.get("xml");
			String destinatario = (String) params.get("destinatario");
			String pathLogo = (String) params.get("pathLogo");
			
			if (idFacturador == null || ambiente == null || conectaSRI == null || 
					clave == null || codigoNotaDebito == null || xml == null) {
				java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
				errorResponse.put("mensaje", "ERROR");
				errorResponse.put("error", "Faltan parámetros obligatorios");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}
			
			String resultado = notaDebitoService.autorizarNotaDebito(idFacturador, ambiente, conectaSRI, 
					clave, codigoNotaDebito, xml, destinatario, pathLogo);
			
			java.util.Map<String, String> response = new java.util.HashMap<>();
			response.put("mensaje", resultado);
			response.put("clave", clave);
			
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

	/**
	 * Anula una nota de débito y su asiento contable vinculado.
	 * Body JSON: { "idNotaDebito": 1, "motivo": "...", "usuario": "..." }
	 */
	@POST
	@Path("/anular")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response anularNotaDebito(java.util.Map<String, Object> params) {
		System.out.println("LLEGA AL SERVICIO anularNotaDebito");
		try {
			Long idNotaDebito = getLongParam(params, "idNotaDebito");
			String motivo  = (String) params.get("motivo");
			String usuario = (String) params.get("usuario");

			if (idNotaDebito == null) {
				java.util.Map<String, Object> err = new java.util.HashMap<>();
				err.put("exito", false);
				err.put("mensaje", "El parámetro 'idNotaDebito' es obligatorio.");
				return Response.status(Response.Status.BAD_REQUEST).entity(err).type(MediaType.APPLICATION_JSON).build();
			}
			if (usuario == null || usuario.trim().isEmpty()) {
				java.util.Map<String, Object> err = new java.util.HashMap<>();
				err.put("exito", false);
				err.put("mensaje", "El parámetro 'usuario' es obligatorio.");
				return Response.status(Response.Status.BAD_REQUEST).entity(err).type(MediaType.APPLICATION_JSON).build();
			}

			java.util.Map<String, Object> resultado = notaDebitoService.anularNotaDebito(idNotaDebito, motivo, usuario);
			boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
			return Response.status(exito ? Response.Status.OK : Response.Status.BAD_REQUEST)
					.entity(resultado).type(MediaType.APPLICATION_JSON).build();

		} catch (Throwable e) {
			System.err.println("ERROR en anularNotaDebito REST: " + e.getMessage());
			e.printStackTrace();
			java.util.Map<String, Object> err = new java.util.HashMap<>();
			err.put("exito", false);
			err.put("mensaje", "Error inesperado al anular la nota de débito: " + e.getMessage());
			err.put("error", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(err).type(MediaType.APPLICATION_JSON).build();
		}
	}
}