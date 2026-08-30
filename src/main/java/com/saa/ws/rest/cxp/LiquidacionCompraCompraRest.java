package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.LiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.service.LiquidacionCompraCompraService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("lqcc")
public class LiquidacionCompraCompraRest {
	@EJB private LiquidacionCompraCompraDaoService liquidacionCompraCompraDaoService;
	@EJB private LiquidacionCompraCompraService liquidacionCompraCompraService;
	@EJB private SustentoTributarioService sustentoTributarioService;
	@Context private UriInfo context;
	public LiquidacionCompraCompraRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<LiquidacionCompraCompra> lista = liquidacionCompraCompraDaoService.selectAll(NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener LiquidacionCompraCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			LiquidacionCompraCompra entidad = liquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("LiquidacionCompraCompra ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
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
		System.out.println("selectByCriteria de LiquidacionCompraCompra");
		try {
			List<LiquidacionCompraCompra> lista = liquidacionCompraCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(LiquidacionCompraCompra registro) {
		System.out.println("LLEGA AL SERVICIO PUT LiquidacionCompraCompra");
		try {
			LiquidacionCompraCompra resultado = liquidacionCompraCompraService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar LiquidacionCompraCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(LiquidacionCompraCompra registro) {
		System.out.println("LLEGA AL SERVICIO POST LiquidacionCompraCompra");
		try {
			LiquidacionCompraCompra resultado = liquidacionCompraCompraService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear LiquidacionCompraCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/delete/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			liquidacionCompraCompraService.remove(java.util.Arrays.asList(id));
			return Response.status(Response.Status.OK).entity("LiquidacionCompraCompra eliminada correctamente").type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Anula una liquidación de compra: {motivo, usuario}. Ver
	 * docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §10.3.
	 */
	@POST @Path("/anular/{id}") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response anular(@PathParam("id") Long id, java.util.Map<String, Object> datos) {
		System.out.println("LLEGA AL SERVICIO POST LiquidacionCompraCompra/anular id: " + id);
		try {
			String motivo = (datos != null && datos.get("motivo") != null) ? datos.get("motivo").toString() : null;
			String usuario = (datos != null && datos.get("usuario") != null) ? datos.get("usuario").toString() : null;
			java.util.Map<String, Object> resultado = liquidacionCompraCompraService.anularLiquidacionCompra(id, motivo, usuario);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al anular LiquidacionCompraCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	// ── ATS: sustento tributario SRI (Tabla 5) de la liquidación de compra ───────

	@GET @Path("/sustento/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getSustento(@PathParam("id") Long id) {
		try {
			LiquidacionCompraCompra entidad = liquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("LiquidacionCompraCompra con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			java.util.Map<String, Object> r = new java.util.HashMap<>();
			r.put("idLiquidacion", id);
			r.put("sustentoTributario", entidad.getSustentoTributario());
			r.put("resuelto", entidad.getSustentoTributario() != null);
			return Response.status(Response.Status.OK).entity(r).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al consultar el sustento de LiquidacionCompraCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT @Path("/sustento/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response corregirSustento(@PathParam("id") Long id, @QueryParam("sustento") String sustento) {
		System.out.println("LLEGA AL SERVICIO PUT LiquidacionCompraCompra/sustento id: " + id + " sustento: " + sustento);
		try {
			LiquidacionCompraCompra resultado = sustentoTributarioService.corregirSustentoLiquidacion(id, sustento);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (com.saa.basico.util.IncomeException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al corregir el sustento de LiquidacionCompraCompra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET @Path("/sustentoPendiente") @Produces(MediaType.APPLICATION_JSON)
	public Response getSustentoPendiente(@QueryParam("idEmpresa") Long idEmpresa) {
		try {
			List<com.saa.model.cxp.FacturaSustentoPendiente> lista = sustentoTributarioService.listarPendientesLiquidacion(idEmpresa);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al listar liquidaciones con sustento pendiente: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
