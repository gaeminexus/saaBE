package com.saa.ws.rest.cxp;

import java.util.HashMap;
import java.util.Map;

import com.saa.basico.ejb.UsuarioDaoService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.service.DocumentoSeguroCxpService;
import com.saa.model.scp.NombreEntidadesSistema;
import com.saa.model.scp.Usuario;

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
 * REST para crédito: documentos de seguros en CxP (docs/logica-negocio/cxp/
 * API-DOCUMENTOS-SEGUROS-CXP.md §5). Delega enteramente en {@link DocumentoSeguroCxpService}, sin
 * lógica propia. Todos responden 400 con texto ante una {@code IncomeException} y 500 con
 * "Error en documentos de seguros: " + mensaje ante lo demás.
 * <p>
 * §5.0.2: el service {@code @Local} recibe el nombre de usuario; este REST recibe
 * {@code idUsuario} y hace esa conversión con {@code UsuarioDaoService.selectByNombre}
 * (indirectamente, resolviendo primero el {@code Usuario} por id y tomando su nombre).
 */
@Path("cxp-seguros")
public class DocumentoSeguroCxpRest {

	@EJB private DocumentoSeguroCxpService documentoSeguroCxpService;
	@EJB private UsuarioDaoService usuarioDaoService;

	public DocumentoSeguroCxpRest() {}

	@GET
	@Path("/porClave/{claveAcceso}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response porClave(@PathParam("claveAcceso") String claveAcceso,
			@QueryParam("idEmpresa") Long idEmpresa) {
		System.out.println("=== REST cxp-seguros/porClave clave=" + claveAcceso + " ===");
		try {
			Map<String, Object> resultado = documentoSeguroCxpService.porClave(claveAcceso, idEmpresa);
			return Response.status(Response.Status.OK)
					.entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (IncomeException e) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(errorMap(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMap("Error en documentos de seguros: " + e.getMessage()))
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("/enlazar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response enlazar(Map<String, Object> datos) {
		System.out.println("=== REST cxp-seguros/enlazar ===");
		try {
			String nombreUsuario = resolverNombreUsuario(datos);
			Map<String, Object> resultado = documentoSeguroCxpService.enlazar(
					(String) datos.get("tipoDocumento"), toLong(datos.get("idDocumento")),
					toLong(datos.get("idDocumentoSeguro")), nombreUsuario);
			return Response.status(Response.Status.OK)
					.entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (IncomeException e) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(errorMap(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMap("Error en documentos de seguros: " + e.getMessage()))
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("/liberar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response liberar(Map<String, Object> datos) {
		System.out.println("=== REST cxp-seguros/liberar ===");
		try {
			String nombreUsuario = resolverNombreUsuario(datos);
			Map<String, Object> resultado = documentoSeguroCxpService.liberar(
					(String) datos.get("tipoDocumento"), toLong(datos.get("idDocumento")),
					toLong(datos.get("idDocumentoSeguro")), nombreUsuario);
			return Response.status(Response.Status.OK)
					.entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (IncomeException e) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(errorMap(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMap("Error en documentos de seguros: " + e.getMessage()))
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("/desenlazar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response desenlazar(Map<String, Object> datos) {
		System.out.println("=== REST cxp-seguros/desenlazar ===");
		try {
			String nombreUsuario = resolverNombreUsuario(datos);
			Map<String, Object> resultado = documentoSeguroCxpService.desenlazar(
					(String) datos.get("tipoDocumento"), toLong(datos.get("idDocumento")),
					toLong(datos.get("idDocumentoSeguro")), nombreUsuario);
			return Response.status(Response.Status.OK)
					.entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (IncomeException e) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(errorMap(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMap("Error en documentos de seguros: " + e.getMessage()))
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	// =====================================================================
	// Helpers
	// =====================================================================

	private String resolverNombreUsuario(Map<String, Object> datos) throws Throwable {
		Long idUsuario = toLong(datos.get("idUsuario"));
		if (idUsuario == null) {
			throw new IncomeException("Debe enviar idUsuario.");
		}
		Usuario usuario = usuarioDaoService.selectById(idUsuario, NombreEntidadesSistema.USUARIO);
		if (usuario == null) {
			throw new IncomeException("No se encontró el usuario con ID: " + idUsuario);
		}
		return usuario.getNombre();
	}

	private Long toLong(Object valor) {
		if (valor == null) return null;
		if (valor instanceof Number) return ((Number) valor).longValue();
		String s = valor.toString().trim();
		return s.isEmpty() ? null : Long.valueOf(s);
	}

	private Map<String, Object> errorMap(String mensaje) {
		Map<String, Object> m = new HashMap<>();
		m.put("error", mensaje);
		return m;
	}
}
