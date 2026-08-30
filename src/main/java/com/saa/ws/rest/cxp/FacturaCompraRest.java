package com.saa.ws.rest.cxp;
import java.util.List;
import java.util.Map;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.ejb.cxp.service.FacturaCompraService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("fctc")
public class FacturaCompraRest {
	@EJB private FacturaCompraDaoService facturaCompraDaoService;
	@EJB private FacturaCompraService facturaCompraService;
	@EJB private SustentoTributarioService sustentoTributarioService;
	@Context private UriInfo context;
	public FacturaCompraRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<FacturaCompra> lista = facturaCompraDaoService.selectAll(NombreEntidadesCompra.FACTURA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener FacturaCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			FacturaCompra entidad = facturaCompraDaoService.selectById(id, NombreEntidadesCompra.FACTURA_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("FacturaCompra con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener FacturaCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		System.out.println("selectByCriteria de FacturaCompra");
		try {
			List<FacturaCompra> lista = facturaCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.FACTURA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(FacturaCompra registro) {
		System.out.println("LLEGA AL SERVICIO PUT FacturaCompra");
		try {
			FacturaCompra resultado = facturaCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar FacturaCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(FacturaCompra registro) {
		System.out.println("LLEGA AL SERVICIO POST FacturaCompra");
		try {
			FacturaCompra resultado = facturaCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear FacturaCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE FacturaCompra id: " + id);
		try {
			facturaCompraService.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("FacturaCompra eliminada correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar FacturaCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Anula una factura de compra: {motivo, usuario, idUsuario, anularEnCascada}.
	 * `idUsuario` solo hace falta si `anularEnCascada=true` y hay movimientos de tipo pago
	 * directo entre lo que se va a reversar. Ver
	 * docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §10.3 y el ítem 13 (anulación en
	 * cascada de movimientos relacionados).
	 */
	@POST @Path("/anular/{id}") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
		System.out.println("LLEGA AL SERVICIO POST FacturaCompra/anular id: " + id);
		try {
			String motivo = (datos != null && datos.get("motivo") != null) ? datos.get("motivo").toString() : null;
			String usuario = (datos != null && datos.get("usuario") != null) ? datos.get("usuario").toString() : null;
			Long idUsuario = (datos != null && datos.get("idUsuario") != null)
					? Long.valueOf(datos.get("idUsuario").toString()) : null;
			boolean anularEnCascada = datos != null && Boolean.TRUE.equals(datos.get("anularEnCascada"));
			Map<String, Object> resultado = facturaCompraService.anularFacturaCompra(
					id, motivo, usuario, idUsuario, anularEnCascada);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (com.saa.basico.util.IncomeException e) {
			return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al anular FacturaCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Movimientos relacionados a una factura de compra (pagos, notas, retenciones, anticipos
	 * cruzados) — para que el frontend muestre la lista antes de preguntar "¿anular con todos
	 * los movimientos?" (ítem 13).
	 */
	@GET @Path("/movimientosRelacionados/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response movimientosRelacionados(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO GET FacturaCompra/movimientosRelacionados id: " + id);
		try {
			List<Map<String, Object>> lista = facturaCompraService.movimientosRelacionadosFactura(id);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al consultar movimientos relacionados: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	// ── T4 (ATS): sustento tributario SRI (Tabla 5) de la factura ────────────────

	@GET @Path("/sustento/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getSustento(@PathParam("id") Long id) {
		try {
			FacturaCompra entidad = facturaCompraDaoService.selectById(id, NombreEntidadesCompra.FACTURA_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("FacturaCompra con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			Map<String, Object> r = new java.util.HashMap<>();
			r.put("idFactura", id);
			r.put("sustentoTributario", entidad.getSustentoTributario());
			r.put("resuelto", entidad.getSustentoTributario() != null);
			return Response.status(Response.Status.OK).entity(r).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al consultar el sustento de FacturaCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT @Path("/sustento/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response corregirSustento(@PathParam("id") Long id, @QueryParam("sustento") String sustento) {
		System.out.println("LLEGA AL SERVICIO PUT FacturaCompra/sustento id: " + id + " sustento: " + sustento);
		try {
			FacturaCompra resultado = sustentoTributarioService.corregirSustento(id, sustento);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (com.saa.basico.util.IncomeException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al corregir el sustento de FacturaCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET @Path("/sustentoPendiente") @Produces(MediaType.APPLICATION_JSON)
	public Response getSustentoPendiente(@QueryParam("idEmpresa") Long idEmpresa) {
		try {
			List<com.saa.model.cxp.FacturaSustentoPendiente> lista = sustentoTributarioService.listarPendientes(idEmpresa);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al listar facturas con sustento pendiente: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET @Path("/sustentoCatalogo") @Produces(MediaType.APPLICATION_JSON)
	public Response getSustentoCatalogo() {
		try {
			Map<String, String> catalogo = sustentoTributarioService.catalogoVigente();
			return Response.status(Response.Status.OK).entity(catalogo).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener el catalogo de sustento tributario: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
