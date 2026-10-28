package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.asoprep.service.CargaArchivoPetroService;
import com.saa.ejb.asoprep.service.ReprocesoCargaPetroService;
import com.saa.ejb.crd.dao.CargaArchivoDaoService;
import com.saa.ejb.crd.service.CargaArchivoService;
import com.saa.ejb.crd.service.dto.ResultadoReprocesoCargaPetro;
import com.saa.ejb.crd.service.dto.SolicitudReprocesoCargaPetro;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.ws.rest.files.FileResponse;

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

@Path("crar")
public class CargaArchivoRest {

    @EJB
    private CargaArchivoDaoService cargaArchivoDaoService;

    @EJB
    private CargaArchivoService cargaArchivoService;
    
    @EJB
    private CargaArchivoPetroService cargaArchivoPetroService;

    /** Cuadre Superintendencia, cambio C4: reproceso de una carga ya aplicada. */
    @EJB
    private ReprocesoCargaPetroService reprocesoCargaPetroService;

    @Context
    private UriInfo context;

    public CargaArchivoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CargaArchivo> lista = cargaArchivoDaoService.selectAll(NombreEntidadesCredito.CARGA_ARCHIVO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cargas de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CargaArchivo cargaArchivo = cargaArchivoDaoService.selectById(id, NombreEntidadesCredito.CARGA_ARCHIVO);
            if (cargaArchivo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CargaArchivo con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cargaArchivo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener carga de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Path("/getByAnio/{anio}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByAnio(@PathParam("anio") Long anio) {
        try {
            List<CargaArchivo> lista = cargaArchivoDaoService.selectByAnio(anio);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cargas por año: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CargaArchivo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CARGAARCHIVO");
        try {
            CargaArchivo resultado = cargaArchivoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar carga de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CargaArchivo registro) {
        System.out.println("LLEGA AL SERVICIO POST - CARGAARCHIVO");
        try {
            CargaArchivo resultado = cargaArchivoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear carga de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CARGAARCHIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cargaArchivoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Reprocesa una carga Petro YA APLICADA (cuadre Superintendencia, cambio C4): anula la
     * aplicación anterior (pagos, aportes y asientos de reparto/aplicación, con trazabilidad,
     * sin borrar) y vuelve a correr la fase 3 vigente fechada al último día del mes de la carga.
     * Diseño y criterio de aceptación: {@code docs/logica-negocio/petro/REPROCESO-CARGA-PETRO.md}.
     *
     * Body: {@code {"usuario": "...", "ip": "...", "motivo": "..."}} — usuario y motivo obligatorios.
     * 409 con el motivo exacto si una precondición de negocio falla (carga sin recepción
     * confirmada, valores sin destino, descuadre recibido/aplicado); nada queda grabado.
     */
    @POST
    @Path("/reprocesar/{idCarga}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reprocesar(@PathParam("idCarga") Long idCarga, SolicitudReprocesoCargaPetro solicitud) {
        System.out.println("REST: REPROCESAR CARGA PETRO - Carga: " + idCarga);
        try {
            ResultadoReprocesoCargaPetro resultado = reprocesoCargaPetroService.reprocesar(idCarga, solicitud);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            System.err.println("Carga no reprocesada: " + e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(new FileResponse(false, e.getMessage(), null))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            System.err.println("ERROR al reprocesar la carga Petro " + idCarga + ":");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al reprocesar la carga: " + e.getMessage(), null))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - CARGAARCHIVO");
        try {
            CargaArchivo elimina = new CargaArchivo();
            cargaArchivoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar carga de archivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
