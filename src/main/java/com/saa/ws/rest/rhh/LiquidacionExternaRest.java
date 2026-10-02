package com.saa.ws.rest.rhh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.service.LiquidacionExternaService;
import com.saa.ejb.rhh.service.dto.SolicitudLiquidacionExterna;
import com.saa.model.rhh.DetalleLiquidacionExterna;
import com.saa.model.rhh.LiquidacionExterna;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST para liquidaciones de ex-colaboradores de la administracion anterior. RHH.
 * Base path: /lqex. Contrato:
 * docs/logica-negocio/rhh/API-LIQUIDACION-EXCOLABORADORES.md §6.
 *
 * <p>Estilo de error de la casa: 500 con el texto del {@code IncomeException} (no 409/400):
 * el contrato lo pide asi explicitamente, a diferencia de otros REST mas nuevos de este
 * mismo modulo que devuelven 409.</p>
 */
@Path("lqex")
public class LiquidacionExternaRest {

	@EJB
	private LiquidacionExternaService liquidacionExternaService;

	public LiquidacionExternaRest() {
	}

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<LiquidacionExterna> lista = liquidacionExternaService.selectAll();
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener LiquidacionExterna: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			LiquidacionExterna entidad = liquidacionExternaService.selectById(id);
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener LiquidacionExterna: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("/selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		try {
			List<LiquidacionExterna> resultado = liquidacionExternaService.selectByCriteria(datos);
			// §6, explicito: sin resultados es [] con 200, no 500 -- selectByCriteria no lanza
			// IncomeException en vacio (a diferencia del resto de la casa).
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error en selectByCriteria LiquidacionExterna: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/detalle/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response detalle(@PathParam("id") Long id) {
		try {
			List<DetalleLiquidacionExterna> resultado = liquidacionExternaService.detalle(id);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener el detalle de la liquidacion: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("/registrar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registrar(SolicitudLiquidacionExterna solicitud) {
		System.out.println("LLEGA AL SERVICIO POST /lqex/registrar");
		try {
			LiquidacionExterna liquidacion = solicitud != null ? solicitud.getLiquidacion() : null;
			List<DetalleLiquidacionExterna> detalles =
					solicitud != null && solicitud.getDetalles() != null ? solicitud.getDetalles()
							: new ArrayList<DetalleLiquidacionExterna>();
			LiquidacionExterna resultado = liquidacionExternaService.registrar(liquidacion, detalles);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON)
					.build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al registrar la liquidacion: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT
	@Path("/actualizar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response actualizar(SolicitudLiquidacionExterna solicitud) {
		System.out.println("LLEGA AL SERVICIO PUT /lqex/actualizar");
		try {
			LiquidacionExterna liquidacion = solicitud != null ? solicitud.getLiquidacion() : null;
			List<DetalleLiquidacionExterna> detalles =
					solicitud != null && solicitud.getDetalles() != null ? solicitud.getDetalles()
							: new ArrayList<DetalleLiquidacionExterna>();
			LiquidacionExterna resultado = liquidacionExternaService.actualizar(liquidacion, detalles);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar la liquidacion: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("/enviarATesoreria/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response enviarATesoreria(@PathParam("id") Long id, Map<String, Object> datos) {
		System.out.println("LLEGA AL SERVICIO POST /lqex/enviarATesoreria/" + id);
		try {
			Long idUsuario = toLong(datos != null ? datos.get("idUsuario") : null);
			Map<String, Object> resultado = liquidacionExternaService.enviarATesoreria(id, idUsuario);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al enviar la liquidacion a tesoreria: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("/sincronizarPago/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response sincronizarPago(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO POST /lqex/sincronizarPago/" + id);
		try {
			LiquidacionExterna resultado = liquidacionExternaService.sincronizarPago(id);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al sincronizar el pago de la liquidacion: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("/anular/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
		System.out.println("LLEGA AL SERVICIO POST /lqex/anular/" + id);
		try {
			Long idUsuario = toLong(datos != null ? datos.get("idUsuario") : null);
			String motivo = datos != null ? (String) datos.get("motivo") : null;
			LiquidacionExterna resultado = liquidacionExternaService.anular(id, idUsuario, motivo);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al anular la liquidacion: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	// ── Helpers ───────────────────────────────────────────────────────────

	private Long toLong(Object valor) {
		if (valor == null) {
			return null;
		}
		if (valor instanceof Number) {
			return ((Number) valor).longValue();
		}
		try {
			return Long.valueOf(valor.toString().trim());
		} catch (Exception e) {
			return null;
		}
	}
}
