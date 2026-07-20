package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.CargaArchivoTxtDaoService;
import com.saa.ejb.cxp.service.CargaArchivoTxtService;
import com.saa.model.cxp.CargaArchivoTxt;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("crtx")
public class CargaArchivoTxtRest {
	@EJB private CargaArchivoTxtDaoService cargaArchivoTxtDaoService;
	@EJB private CargaArchivoTxtService cargaArchivoTxtService;
	@Context private UriInfo context;
	public CargaArchivoTxtRest() {}
	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<CargaArchivoTxt> lista = cargaArchivoTxtDaoService.selectAll(NombreEntidadesCompra.CARGA_ARCHIVO_TXT);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			CargaArchivoTxt entidad = cargaArchivoTxtDaoService.selectById(id, NombreEntidadesCompra.CARGA_ARCHIVO_TXT);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("CargaArchivoTxt ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> registros) {
		System.out.println("selectByCriteria de CargaArchivoTxt");
		try {
			return Response.status(Response.Status.OK)
					.entity(cargaArchivoTxtService.selectByCriteria(registros))
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	/**
	 * Obtiene todas las cargas de una empresa, ordenadas por id desc.
	 * GET /crtx/getByEmpresa/{idEmpresa}
	 */
	@GET @Path("/getByEmpresa/{idEmpresa}") @Produces(MediaType.APPLICATION_JSON)
	public Response getByEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
		try {
			List<CargaArchivoTxt> lista = cargaArchivoTxtService.selectByEmpresa(idEmpresa);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(CargaArchivoTxt registro) {
		try {
			CargaArchivoTxt resultado = cargaArchivoTxtService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(CargaArchivoTxt registro) {
		try {
			CargaArchivoTxt resultado = cargaArchivoTxtService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
	@DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE - CargaArchivoTxt");
		try {
			CargaArchivoTxt elimina = new CargaArchivoTxt();
			cargaArchivoTxtDaoService.remove(elimina, id);
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}
