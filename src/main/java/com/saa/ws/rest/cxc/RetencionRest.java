package com.saa.ws.rest.cxc;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.RetencionDaoService;
import com.saa.ejb.cxc.service.RetencionService;
import com.saa.model.cxc.Retencion;
import com.saa.model.cxc.NombreEntidadesCobro;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("rtnc")
public class RetencionRest {
	@EJB
	private RetencionDaoService retencionDaoService;
	@EJB
	private RetencionService retencionService;
	@Context
	private UriInfo context;
	public RetencionRest() {}
	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<Retencion> lista = retencionDaoService.selectAll(NombreEntidadesCobro.RETENCION);
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
			Retencion entidad = retencionDaoService.selectById(id, NombreEntidadesCobro.RETENCION);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND).entity("Retencion ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(Retencion registro) {
		System.out.println("LLEGA AL SERVICIO PUT Retencion");
		try {
			Retencion resultado = retencionService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(Retencion registro) {
		System.out.println("LLEGA AL SERVICIO POST Retencion");
		try {
			Retencion resultado = retencionService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	
	/**
	 * ENDPOINT PRINCIPAL: Procesa un comprobante de retención completo automáticamente.
	 * Este endpoint ejecuta todo el flujo: graba, genera XML, firma y autoriza ante el SRI.
	 * 
	 * El frontend solo debe enviar:
	 * {
	 *   "retencion": { objeto Retencion con todos los datos }
	 * }
	 * 
	 * Configuración automática:
	 * - ambiente: 1 (PRUEBA)
	 * - conectaSRI: 1 (SI)
	 * - destinatario: se obtiene del campo mail del proveedor
	 * - pathLogo: resources/logos/logo_aso.png
	 * 
	 * @param params Mapa con el objeto retencion
	 * @return JSON con el resultado del proceso completo
	 */
	@POST
	@Path("/procesarCompleta")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response procesarRetencionCompleta(java.util.Map<String, Object> params) {
		System.out.println("=== LLEGA AL SERVICIO procesarRetencionCompleta ===");
		try {
			// Extraer parámetros del JSON
			@SuppressWarnings("unchecked")
			java.util.Map<String, Object> retencionMap = (java.util.Map<String, Object>) params.get("retencion");
			
			// Validar parámetro obligatorio
			if (retencionMap == null) {
				java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
				errorResponse.put("mensaje", "ERROR");
				errorResponse.put("error", "Parámetro 'retencion' es obligatorio");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}
			
			// Convertir el Map a objeto Retencion
			Retencion retencion = convertMapToRetencion(retencionMap);
			
			// Llamar al servicio que ejecuta todo el proceso
			java.util.Map<String, Object> resultado = retencionService.procesarRetencionCompleta(
				retencion,
				null,  // detalles (sin detalles en esta ruta)
				null,  // ambiente se configura automáticamente en el servicio
				null,  // conectaSRI se configura automáticamente en el servicio
				null,  // destinatario se obtiene del proveedor
				null   // pathLogo se construye automáticamente
			);
			
			// Retornar resultado
			return Response.status(Response.Status.OK)
					.entity(resultado)
					.type(MediaType.APPLICATION_JSON).build();
					
		} catch (Throwable e) {
			System.err.println("ERROR en procesarRetencionCompleta REST: " + e.getMessage());
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
	 * Convierte un Map a objeto Retencion.
	 */
	private Retencion convertMapToRetencion(java.util.Map<String, Object> map) {
		com.fasterxml.jackson.databind.ObjectMapper mapper = createObjectMapper();
		return mapper.convertValue(map, Retencion.class);
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
		System.out.println("LLEGA AL SERVICIO DELETE Retencion con id: " + id);
		try {
			List<Long> ids = new java.util.ArrayList<>();
			ids.add(id);
			retencionService.remove(ids);
			return Response.status(Response.Status.OK).entity("Retencion eliminada correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("LLEGA AL SERVICIO selectByCriteria Retencion");
		try {
			List<Retencion> result = retencionService.selectByCriteria(datos);
			return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	
	/**
	 * Genera el XML de comprobante de retención electrónica según estándares del SRI v1.0.0
	 * @param clave Clave de acceso de la retención
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return JSON con el resultado de la generación
	 */
	@GET
	@Path("/generarXML/{clave}/{ambiente}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response generarXML(@PathParam("clave") String clave, @PathParam("ambiente") Long ambiente) {
		System.out.println("LLEGA AL SERVICIO generarXML Retencion con clave: " + clave + " y ambiente: " + ambiente);
		try {
			String[] resultado = retencionService.generarXMLRetencion(clave, ambiente);
			
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
	 * Autoriza el comprobante de retención electrónica ante el SRI
	 * @param params Mapa con los parámetros de autorización
	 * @return JSON con el resultado de la autorización
	 */
	@POST
	@Path("/autorizar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response autorizarRetencion(java.util.Map<String, Object> params) {
		System.out.println("LLEGA AL SERVICIO autorizarRetencion");
		try {
			Long idFacturador = getLongParam(params, "idFacturador");
			Long ambiente = getLongParam(params, "ambiente");
			Long conectaSRI = getLongParam(params, "conectaSRI");
			String clave = (String) params.get("clave");
			Long codigoRetencion = getLongParam(params, "idRetencion");
			String xml = (String) params.get("xml");
			String destinatario = (String) params.get("destinatario");
			String pathLogo = (String) params.get("pathLogo");
			
			if (idFacturador == null || ambiente == null || conectaSRI == null || 
					clave == null || codigoRetencion == null || xml == null) {
				java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
				errorResponse.put("mensaje", "ERROR");
				errorResponse.put("error", "Faltan parámetros obligatorios");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}
			
			String resultado = retencionService.autorizarRetencion(idFacturador, ambiente, conectaSRI, 
					clave, codigoRetencion, xml, destinatario, pathLogo);
			
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
}