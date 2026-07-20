package com.saa.ws.rest.cxp;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.DocumentoCxpDaoService;
import com.saa.ejb.cxp.service.DocumentoCxpService;
import com.saa.model.cxp.DocumentoCxp;
import com.saa.model.cxp.NombreEntidadesCompra;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("dcxp")
public class DocumentoCxpRest {
	
	@EJB private DocumentoCxpDaoService documentoCxpDaoService;
	@EJB private DocumentoCxpService documentoCxpService;
	@Context private UriInfo context;
	public DocumentoCxpRest() {}

	@GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<DocumentoCxp> lista = documentoCxpDaoService.selectAll(NombreEntidadesCompra.DOCUMENTO_CXP);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			DocumentoCxp entidad = documentoCxpDaoService.selectById(id, NombreEntidadesCompra.DOCUMENTO_CXP);
			if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("DocumentoCxp ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> registros) {
		System.out.println("selectByCriteria de DocumentoCxp");
		try {
			return Response.status(Response.Status.OK)
					.entity(documentoCxpService.selectByCriteria(registros))
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Obtiene todos los documentos de una empresa, ordenados por id desc.
	 * GET /dcxp/getByEmpresa/{idEmpresa}
	 */
	@GET @Path("/getByEmpresa/{idEmpresa}") @Produces(MediaType.APPLICATION_JSON)
	public Response getByEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
		try {
			List<DocumentoCxp> lista = documentoCxpService.selectByEmpresa(idEmpresa);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Obtiene documentos de una empresa filtrados por estadoDocumento.
	 * GET /dcxp/getByEmpresaEstado/{idEmpresa}/{estado}
	 * estados: 1=LEIDO 2=XML_CARGADO 3=REGISTRADO_BD 4=ERROR 5=NOVEDAD 6=REVERTIDO
	 */
	@GET @Path("/getByEmpresaEstado/{idEmpresa}/{estado}") @Produces(MediaType.APPLICATION_JSON)
	public Response getByEmpresaEstado(@PathParam("idEmpresa") Long idEmpresa,
	                                    @PathParam("estado") Long estado) {
		try {
			List<DocumentoCxp> lista = documentoCxpService.selectByEmpresaEstado(idEmpresa, estado);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Obtiene documentos con novedad pendiente de resolución de una empresa.
	 * GET /dcxp/novedadesPendientes/{idEmpresa}
	 * estadoDocumento=5 y estadoNovedad=1
	 */
	@GET @Path("/novedadesPendientes/{idEmpresa}") @Produces(MediaType.APPLICATION_JSON)
	public Response novedadesPendientes(@PathParam("idEmpresa") Long idEmpresa) {
		try {
			List<DocumentoCxp> lista = documentoCxpService.selectNovedadesPendientes(idEmpresa);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response put(DocumentoCxp registro) {
		try {
			DocumentoCxp resultado = documentoCxpService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
	public Response post(DocumentoCxp registro) {
		try {
			DocumentoCxp resultado = documentoCxpService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}

	@DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		System.out.println("LLEGA AL SERVICIO DELETE - DocumentoCxp");
		try {
			DocumentoCxp elimina = new DocumentoCxp();
			documentoCxpDaoService.remove(elimina, id);
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
	}
}