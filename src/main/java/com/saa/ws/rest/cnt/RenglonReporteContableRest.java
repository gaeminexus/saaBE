package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.RenglonReporteContableDaoService;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.RenglonReporteContable;

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
 * C19b — CRUD de los renglones de un reporte contable de renglones propios (CNT.RNRP), igual que /dtrp para las
 * reglas. Permite parametrizar el B17, el flujo de caja o cualquier reporte de renglones sin tocar codigo.
 */
@Path("rnrp")
public class RenglonReporteContableRest {

	@EJB
	private RenglonReporteContableDaoService renglonDaoService;

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			return Response.ok(renglonDaoService.selectAll(NombreEntidadesContabilidad.RENGLON_REPORTE_CONTABLE))
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			RenglonReporteContable r = renglonDaoService.selectById(id, NombreEntidadesContabilidad.RENGLON_REPORTE_CONTABLE);
			if (r == null) {
				return Response.status(Response.Status.NOT_FOUND).entity("Renglon " + id + " no encontrado")
						.type(MediaType.APPLICATION_JSON).build();
			}
			return Response.ok(r).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> registros) {
		try {
			return Response.ok(renglonDaoService.selectByCriteria(registros, NombreEntidadesContabilidad.RENGLON_REPORTE_CONTABLE))
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(RenglonReporteContable registro) {
		try {
			return Response.ok(renglonDaoService.save(registro, registro.getCodigo())).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(RenglonReporteContable registro) {
		try {
			return Response.status(Response.Status.CREATED).entity(renglonDaoService.save(registro, registro.getCodigo()))
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			renglonDaoService.remove(new RenglonReporteContable(), id);
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (Throwable e) {
			return error(e);
		}
	}

	private Response error(Throwable e) {
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en renglones de reporte: " + e.getMessage())
				.type(MediaType.APPLICATION_JSON).build();
	}
}
