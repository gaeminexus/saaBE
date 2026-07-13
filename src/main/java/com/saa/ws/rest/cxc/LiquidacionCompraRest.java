package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.LiquidacionCompraDaoService;
import com.saa.ejb.cxc.service.LiquidacionCompraService;
import com.saa.model.cxc.LiquidacionCompra;
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

@Path("lqcs")
public class LiquidacionCompraRest {

	@EJB
	private LiquidacionCompraDaoService liquidacionCompraDaoService;

	@EJB
	private LiquidacionCompraService liquidacionCompraService;

	@Context
	private UriInfo context;

	public LiquidacionCompraRest() {}

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<LiquidacionCompra> lista = liquidacionCompraDaoService.selectAll(NombreEntidadesCobro.LIQUIDACION_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener LiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			LiquidacionCompra entidad = liquidacionCompraDaoService.selectById(id, NombreEntidadesCobro.LIQUIDACION_COMPRA);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("LiquidacionCompra con ID " + id + " no encontrada")
						.type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener LiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(LiquidacionCompra registro) {
		System.out.println("LLEGA AL SERVICIO PUT LiquidacionCompra");
		try {
			LiquidacionCompra resultado = liquidacionCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar LiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(LiquidacionCompra registro) {
		System.out.println("LLEGA AL SERVICIO POST LiquidacionCompra");
		try {
			LiquidacionCompra resultado = liquidacionCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al crear LiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * ENDPOINT PRINCIPAL: Procesa una liquidación de compra completa automáticamente.
	 * Este endpoint ejecuta todo el flujo: graba, genera XML, firma y autoriza ante el SRI.
	 * 
	 * El frontend solo debe enviar:
	 * {
	 *   "liquidacionCompra": { objeto LiquidacionCompra con todos los datos }
	 * }
	 * 
	 * Configuración automática:
	 * - ambiente: 1 (PRUEBA)
	 * - conectaSRI: 1 (SI)
	 * - destinatario: se obtiene del campo mail del proveedor
	 * - pathLogo: resources/logos/logo_aso.png
	 * 
	 * @param params Mapa con el objeto liquidacionCompra
	 * @return JSON con el resultado del proceso completo
	 */
	@POST
	@Path("/procesarCompleta")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response procesarLiquidacionCompleta(java.util.Map<String, Object> params) {
		System.out.println("=== LLEGA AL SERVICIO procesarLiquidacionCompleta ===");
		try {
			// Extraer parámetros del JSON
			@SuppressWarnings("unchecked")
			java.util.Map<String, Object> liquidacionMap = (java.util.Map<String, Object>) params.get("liquidacionCompra");
			
			// Validar parámetro obligatorio
			if (liquidacionMap == null) {
				java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
				errorResponse.put("mensaje", "ERROR");
				errorResponse.put("error", "Parámetro 'liquidacionCompra' es obligatorio");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}
			
			// Convertir el Map a objeto LiquidacionCompra
			LiquidacionCompra liquidacion = convertMapToLiquidacionCompra(liquidacionMap);
			
			// Llamar al servicio que ejecuta todo el proceso
			java.util.Map<String, Object> resultado = liquidacionCompraService.procesarLiquidacionCompleta(
				liquidacion,
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
			System.err.println("ERROR en procesarLiquidacionCompleta REST: " + e.getMessage());
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
	 * Convierte un Map a objeto LiquidacionCompra.
	 */
	private LiquidacionCompra convertMapToLiquidacionCompra(java.util.Map<String, Object> map) {
		com.fasterxml.jackson.databind.ObjectMapper mapper = createObjectMapper();
		return mapper.convertValue(map, LiquidacionCompra.class);
	}
	
	/**
	 * Crea y configura un ObjectMapper con soporte para Java 8 date/time
	 * e ignorando propiedades desconocidas.
	 */
	private com.fasterxml.jackson.databind.ObjectMapper createObjectMapper() {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
		mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE LiquidacionCompra con id: " + id);
		try {
			List<Long> ids = new java.util.ArrayList<>();
			ids.add(id);
			liquidacionCompraService.remove(ids);
			return Response.status(Response.Status.OK)
					.entity("LiquidacionCompra eliminada correctamente")
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al eliminar LiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("LLEGA AL SERVICIO selectByCriteria LiquidacionCompra");
		try {
			List<LiquidacionCompra> result = liquidacionCompraService.selectByCriteria(datos);
			return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error en selectByCriteria LiquidacionCompra: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}
	
	/**
	 * Genera el XML de liquidación de compra electrónica según estándares del SRI v1.0.0
	 * @param clave Clave de acceso de la liquidación
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return JSON con el resultado de la generación
	 */
	@GET
	@Path("/generarXML/{clave}/{ambiente}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response generarXML(@PathParam("clave") String clave, @PathParam("ambiente") Long ambiente) {
		System.out.println("LLEGA AL SERVICIO generarXML LiquidacionCompra con clave: " + clave + " y ambiente: " + ambiente);
		try {
			String[] resultado = liquidacionCompraService.generarXMLLiquidacion(clave, ambiente);
			
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
	 * Autoriza la liquidación de compra electrónica ante el SRI
	 * @param params Mapa con los parámetros de autorización
	 * @return JSON con el resultado de la autorización
	 */
	@POST
	@Path("/autorizar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response autorizarLiquidacion(java.util.Map<String, Object> params) {
		System.out.println("LLEGA AL SERVICIO autorizarLiquidacion");
		try {
			Long idFacturador = getLongParam(params, "idFacturador");
			Long ambiente = getLongParam(params, "ambiente");
			Long conectaSRI = getLongParam(params, "conectaSRI");
			String clave = (String) params.get("clave");
			Long codigoLiquidacion = getLongParam(params, "idLiquidacion");
			String xml = (String) params.get("xml");
			String destinatario = (String) params.get("destinatario");
			String pathLogo = (String) params.get("pathLogo");
			
			if (idFacturador == null || ambiente == null || conectaSRI == null || 
					clave == null || codigoLiquidacion == null || xml == null) {
				java.util.Map<String, String> errorResponse = new java.util.HashMap<>();
				errorResponse.put("mensaje", "ERROR");
				errorResponse.put("error", "Faltan parámetros obligatorios");
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(errorResponse)
						.type(MediaType.APPLICATION_JSON).build();
			}
			
			String resultado = liquidacionCompraService.autorizarLiquidacion(idFacturador, ambiente, conectaSRI, 
					clave, codigoLiquidacion, xml, destinatario, pathLogo);
			
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