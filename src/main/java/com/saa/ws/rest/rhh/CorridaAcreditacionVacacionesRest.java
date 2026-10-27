package com.saa.ws.rest.rhh;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.CorridaAcreditacionVacacionesDaoService;
import com.saa.ejb.rhh.service.CorridaAcreditacionVacacionesService;
import com.saa.model.rhh.CorridaAcreditacionVacaciones;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Bitacora de las corridas de acreditacion de vacaciones (RHH.CRVC).
 *
 * <p>Es de SOLO LECTURA a proposito: las filas las escribe el sistema al acreditar, y una
 * bitacora que se puede editar o borrar desde la pantalla deja de servir como constancia. Por
 * eso no tiene POST, PUT ni DELETE.</p>
 */
@Path("crvc")
public class CorridaAcreditacionVacacionesRest {

    @EJB
    private CorridaAcreditacionVacacionesDaoService corridaAcreditacionVacacionesDaoService;

    @EJB
    private CorridaAcreditacionVacacionesService corridaAcreditacionVacacionesService;

    @Context
    private UriInfo context;

    public CorridaAcreditacionVacacionesRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GETALL - CORRIDA_ACREDITACION_VACACIONES");
        try {
            List<CorridaAcreditacionVacaciones> lista = corridaAcreditacionVacacionesDaoService
                    .selectAll(NombreEntidadesRhh.CORRIDA_ACREDITACION_VACACIONES);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GETID - CORRIDA_ACREDITACION_VACACIONES");
        try {
            CorridaAcreditacionVacaciones registro = corridaAcreditacionVacacionesDaoService.selectById(id,
                    NombreEntidadesRhh.CORRIDA_ACREDITACION_VACACIONES);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Registro con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CORRIDA_ACREDITACION_VACACIONES");
        try {
            List<CorridaAcreditacionVacaciones> lista = corridaAcreditacionVacacionesService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en búsqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
