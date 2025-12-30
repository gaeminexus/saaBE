package com.saa.ws.rest.basico;

import java.util.List;

import com.saa.basico.ejb.UsuarioDaoService;
import com.saa.model.scp.NombreEntidadesSistema;
import com.saa.model.scp.Usuario;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("usro")
public class UsuarioRest {
	
	@EJB
	private UsuarioDaoService usuarioDaoService;
	
    @Context
    private UriInfo context;

    /**
     * Default constructor. 
     */
    public UsuarioRest() {
        // TODO Auto-generated constructor stub
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            Usuario usuario = usuarioDaoService.selectById(id, NombreEntidadesSistema.USUARIO);
            if (usuario == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Usuario con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(usuario).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener usuario: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getByNombre/{nombre}")
    public Response getByNombre(@PathParam("nombre") String nombre) {
        try {
            Usuario usuario = usuarioDaoService.selectByNombre(nombre);
            if (usuario == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Usuario con nombre " + nombre + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(usuario).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener usuario: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Retrieves representation of an instance of AnioMortorRest
     * @return an instance of String
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getAll")
    public Response getAll() {
        try {
            List<Usuario> lista = usuarioDaoService.selectUsuariosActivos();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener usuarios: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    @Path("/validaUsuario/{idUsuario}/{clave}")
    public Response validaUsuario(@PathParam("idUsuario") String idUsuario, @PathParam("clave") String clave) {
        try {
            String resultado = usuarioDaoService.validaUsuario(idUsuario, clave);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.TEXT_PLAIN).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al validar usuario: " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }
    }
    
    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    @Path("/validaUsuarioSucursal/{idUsuario}/{clave}/{idEmpresa}")
    public Response validaUsuarioSucursal(@PathParam("idUsuario") String idUsuario, @PathParam("clave") String clave, @PathParam("idEmpresa") String idEmpresa) {
        try {
            String resultado = usuarioDaoService.validaUsuarioSucursal(idUsuario, clave, idEmpresa);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.TEXT_PLAIN).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al validar usuario: " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }
    }
    
    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    @Path("/cambiaClave/{idUsuario}/{anterior}/{nueva}")
    public Response cambiaClave(@PathParam("idUsuario") String idUsuario, @PathParam("anterior") String anterior,
    		@PathParam("nueva") String nueva) throws Throwable {
    	String resultado = usuarioDaoService.cambiaClave(idUsuario, anterior, nueva);
    	Response respuesta = null; 
   		respuesta = Response.status(Response.Status.OK).entity(resultado).type(MediaType.TEXT_PLAIN).build();
    	return respuesta;
    }
    
    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    @Path("/verificaPermiso/{idEmpresa}/{idUsuario}/{idPermiso}")
    public Response verificaPermiso(@PathParam("idEmpresa") Long idEmpresa, 
    		@PathParam("idUsuario") Long idUsuario, @PathParam("idPermiso") Long idPermiso) throws Throwable {
    	String resultado = usuarioDaoService.verificaPermiso(idEmpresa, idUsuario, idPermiso);
    	Response respuesta = null; 
   		respuesta = Response.status(Response.Status.OK).entity(resultado).type(MediaType.TEXT_PLAIN).build();
    	return respuesta;
    }

}
