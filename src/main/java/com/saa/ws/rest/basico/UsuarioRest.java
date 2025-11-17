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
    @Produces("application/json")
    @Path("/getId/{id}")
    public Usuario getId(@PathParam("id") Long id) throws Throwable {
    	return usuarioDaoService.selectById(id, NombreEntidadesSistema.USUARIO);
    }
    
    @GET
    @Produces("application/json")
    @Path("/getByNombre/{nombre}")
    public Usuario getByNombre(@PathParam("nombre") String nombre) throws Throwable {
    	return usuarioDaoService.selectByNombre(nombre);
    }

    /**
     * Retrieves representation of an instance of AnioMortorRest
     * @return an instance of String
     * @throws Throwable 
     */
    @GET
    @Produces("application/json")
    @Path("/getAll")
    public List<Usuario> getAll() throws Throwable {
        return usuarioDaoService.selectUsuariosActivos();
    }
    
    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    @Path("/validaUsuario/{idUsuario}/{clave}")
    public Response validaUsuario(@PathParam("idUsuario") String idUsuario, @PathParam("clave") String clave) throws Throwable {
    	String resultado = usuarioDaoService.validaUsuario(idUsuario, clave);
    	Response respuesta = null; 
   		respuesta = Response.status(Response.Status.OK).entity(resultado).type(MediaType.TEXT_PLAIN).build();
    	return respuesta;
    }
    
    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    @Path("/validaUsuarioSucursal/{idUsuario}/{clave}/{idEmpresa}")
    public Response validaUsuarioSucursal(@PathParam("idUsuario") String idUsuario, @PathParam("clave") String clave, @PathParam("idEmpresa") String idEmpresa) throws Throwable {
    	String resultado = usuarioDaoService.validaUsuarioSucursal(idUsuario, clave, idEmpresa);
    	Response respuesta = null; 
   		respuesta = Response.status(Response.Status.OK).entity(resultado).type(MediaType.TEXT_PLAIN).build();
    	return respuesta;
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
