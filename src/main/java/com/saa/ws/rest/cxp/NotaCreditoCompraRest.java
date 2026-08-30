package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.ejb.cxp.service.NotaCreditoCompraService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("ntcc")
public class NotaCreditoCompraRest {
	@EJB private NotaCreditoCompraDaoService notaCreditoCompraDaoService;
	@EJB private NotaCreditoCompraService notaCreditoCompraService;
	@EJB private SustentoTributarioService sustentoTributarioService;
	@Context private UriInfo context;
	public NotaCreditoCompraRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<NotaCreditoCompra> lista = notaCreditoCompraDaoService.selectAll(NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			NotaCreditoCompra entidad = notaCreditoCompraDaoService.selectById(id, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("NotaCreditoCompra ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("selectByCriteria de NotaCreditoCompra");
		try {
			List<NotaCreditoCompra> lista = notaCreditoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(NotaCreditoCompra registro) {
		System.out.println("LLEGA AL SERVICIO PUT NotaCreditoCompra");
		try {
			NotaCreditoCompra resultado = notaCreditoCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar NotaCreditoCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(NotaCreditoCompra registro) {
		System.out.println("LLEGA AL SERVICIO POST NotaCreditoCompra");
		try {
			NotaCreditoCompra resultado = notaCreditoCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear NotaCreditoCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			notaCreditoCompraService.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("NotaCreditoCompra eliminada correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Anula una nota de crédito de compra: {motivo, usuario, idUsuario, anularEnCascada}. Ver
	 * docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §10.3 y el ítem 13.
	 */
	@POST @Path("/anular/{id}") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response anular(@PathParam("id") Long id, java.util.Map<String, Object> datos) {
		System.out.println("LLEGA AL SERVICIO POST NotaCreditoCompra/anular id: " + id);
		try {
			String motivo = (datos != null && datos.get("motivo") != null) ? datos.get("motivo").toString() : null;
			String usuario = (datos != null && datos.get("usuario") != null) ? datos.get("usuario").toString() : null;
			Long idUsuario = (datos != null && datos.get("idUsuario") != null)
					? Long.valueOf(datos.get("idUsuario").toString()) : null;
			boolean anularEnCascada = datos != null && Boolean.TRUE.equals(datos.get("anularEnCascada"));
			java.util.Map<String, Object> resultado = notaCreditoCompraService.anularNotaCreditoCompra(
					id, motivo, usuario, idUsuario, anularEnCascada);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (com.saa.basico.util.IncomeException e) {
			return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al anular NotaCreditoCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Facturas de compra que esta nota de crédito está pagando actualmente — para mostrar
	 * antes de preguntar "¿anular con todos los movimientos?" (ítem 13).
	 */
	@GET @Path("/movimientosRelacionados/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response movimientosRelacionados(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GET NotaCreditoCompra/movimientosRelacionados id: " + id);
		try {
			List<java.util.Map<String, Object>> lista = notaCreditoCompraService.movimientosRelacionadosNotaCredito(id);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al consultar movimientos relacionados: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	// ── ATS: sustento tributario SRI (Tabla 5) de la nota de crédito de compra ───

	@GET @Path("/sustento/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getSustento(@PathParam("id") Long id) {
		try {
			NotaCreditoCompra entidad = notaCreditoCompraDaoService.selectById(id, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("NotaCreditoCompra con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			java.util.Map<String, Object> r = new java.util.HashMap<>();
			r.put("idNotaCredito", id);
			r.put("sustentoTributario", entidad.getSustentoTributario());
			r.put("resuelto", entidad.getSustentoTributario() != null);
			return Response.status(Response.Status.OK).entity(r).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al consultar el sustento de NotaCreditoCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT @Path("/sustento/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response corregirSustento(@PathParam("id") Long id, @QueryParam("sustento") String sustento) {
		System.out.println("LLEGA AL SERVICIO PUT NotaCreditoCompra/sustento id: " + id + " sustento: " + sustento);
		try {
			NotaCreditoCompra resultado = sustentoTributarioService.corregirSustentoNotaCredito(id, sustento);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (com.saa.basico.util.IncomeException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al corregir el sustento de NotaCreditoCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET @Path("/sustentoPendiente") @Produces(MediaType.APPLICATION_JSON)
	public Response getSustentoPendiente(@QueryParam("idEmpresa") Long idEmpresa) {
		try {
			List<com.saa.model.cxp.FacturaSustentoPendiente> lista = sustentoTributarioService.listarPendientesNotaCredito(idEmpresa);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al listar notas de credito con sustento pendiente: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
