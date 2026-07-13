package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.service.FormaPagoLiquidacionService;
import com.saa.model.cxc.FormaPagoLiquidacion;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Servicio REST para gestionar las formas de pago de liquidaciones de compra.
 */
@Path("/formas-pago-liquidacion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FormaPagoLiquidacionRest {

	@EJB
	private FormaPagoLiquidacionService formaPagoLiquidacionService;

	/**
	 * Obtiene todas las formas de pago de liquidaciones
	 */
	@GET
	public Response getAll() {
		try {
			List<FormaPagoLiquidacion> lista = formaPagoLiquidacionService.selectAll();
			return Response.ok(lista).build();
		} catch (Throwable e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("{\"error\":\"" + e.getMessage() + "\"}").build();
		}
	}

	/**
	 * Obtiene una forma de pago por ID
	 */
	@GET
	@Path("/{id}")
	public Response getById(@PathParam("id") Long id) {
		try {
			FormaPagoLiquidacion formaPago = formaPagoLiquidacionService.selectById(id);
			return Response.ok(formaPago).build();
		} catch (Throwable e) {
			e.printStackTrace();
			return Response.status(Response.Status.NOT_FOUND)
					.entity("{\"error\":\"" + e.getMessage() + "\"}").build();
		}
	}

	/**
	 * Busca formas de pago por criterio
	 */
	@POST
	@Path("/buscar")
	public Response buscarPorCriterio(List<DatosBusqueda> criterios) {
		try {
			List<FormaPagoLiquidacion> lista = formaPagoLiquidacionService.selectByCriteria(criterios);
			return Response.ok(lista).build();
		} catch (Throwable e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("{\"error\":\"" + e.getMessage() + "\"}").build();
		}
	}

	/**
	 * Crea una nueva forma de pago
	 */
	@POST
	public Response crear(FormaPagoLiquidacion formaPago) {
		try {
			FormaPagoLiquidacion nueva = formaPagoLiquidacionService.saveSingle(formaPago);
			return Response.status(Response.Status.CREATED).entity(nueva).build();
		} catch (Throwable e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("{\"error\":\"" + e.getMessage() + "\"}").build();
		}
	}

	/**
	 * Actualiza una forma de pago existente
	 */
	@PUT
	@Path("/{id}")
	public Response actualizar(@PathParam("id") Long id, FormaPagoLiquidacion formaPago) {
		try {
			formaPago.setId(id);
			FormaPagoLiquidacion actualizada = formaPagoLiquidacionService.saveSingle(formaPago);
			return Response.ok(actualizada).build();
		} catch (Throwable e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("{\"error\":\"" + e.getMessage() + "\"}").build();
		}
	}

	/**
	 * Elimina una o varias formas de pago
	 */
	@DELETE
	public Response eliminar(List<Long> ids) {
		try {
			formaPagoLiquidacionService.remove(ids);
			return Response.ok("{\"mensaje\":\"Formas de pago eliminadas correctamente\"}").build();
		} catch (Throwable e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("{\"error\":\"" + e.getMessage() + "\"}").build();
		}
	}
}
