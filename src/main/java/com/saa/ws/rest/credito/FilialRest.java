package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.FilialDaoService;
import com.saa.ejb.credito.service.FilialService;
import com.saa.model.credito.Filial;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("flll")
public class FilialRest {
    
    @EJB
    private FilialDaoService filialDaoService;
    
    @EJB
    private FilialService filialService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public FilialRest() {
        // Constructor vacío
    }
    
    /**
     * Método de prueba simple para verificar conectividad
     */
    @GET
    @Path("/test")
    @Produces("application/json")
    public String test() {
        return "{\"status\":\"OK\",\"message\":\"FilialRest está funcionando correctamente\",\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}";
    }
    
    /**
     * Método de diagnóstico para verificar inyección de EJBs
     */
    @GET
    @Path("/diagnostico")
    @Produces("application/json")
    public String diagnostico() {
        StringBuilder resultado = new StringBuilder();
        resultado.append("{\"status\":\"OK\",");
        resultado.append("\"ejbDaoInyectado\":").append(filialDaoService != null).append(",");
        resultado.append("\"ejbServiceInyectado\":").append(filialService != null).append(",");
        resultado.append("\"timestamp\":\"").append(java.time.LocalDateTime.now()).append("\"}");
        return resultado.toString();
    }
    
    /**
     * Obtiene todos los registros de Filial.
     * 
     * @return Lista de Filial
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Filial> getAll() throws Throwable {
        return filialDaoService.selectAll(NombreEntidadesCredito.FILIAL);
    }
    
    /**
     * Obtiene un registro de Filial por su ID.
     * 
     * @param id Identificador del registro
     * @return Objeto Filial
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public Filial getId(@PathParam("id") Long id) throws Throwable {
        return filialDaoService.selectById(id, NombreEntidadesCredito.FILIAL);
    }
    
    /**
     * Crea o actualiza un registro de Filial (PUT).
     * 
     * @param registro Objeto Filial
     * @return Registro actualizado o creado
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public Filial put(Filial registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE Filial");
        return filialService.saveSingle(registro);
    }
    
    /**
     * Crea o actualiza un registro de Filial (POST).
     * 
     * @param registro Objeto Filial
     * @return Registro creado o actualizado
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public Filial post(Filial registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE Filial");
        return filialService.saveSingle(registro);
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Filial");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(filialService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return respuesta;
    }

    
    /**
     * Elimina un registro de Filial por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE Filial");
        Filial elimina = new Filial();
        filialDaoService.remove(elimina, id);
    }
}