package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.BandaProductoDaoService;
import com.saa.ejb.crd.service.BandaProductoService;
import com.saa.model.crd.BandaProducto;
import com.saa.model.crd.NombreEntidadesCredito;

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

/**
 * Bandas de cartera de una configuración (CRD.BNDP).
 *
 * <b>La pantalla de parametrización NO usa estos endpoints para grabar.</b> Las bandas se
 * graban como juego completo por {@code POST /rest/cbpr/guardarConfiguracion}, que es
 * quien valida consecutividad, banda abierta y cuentas. Una banda suelta grabada por aquí
 * puede dejar la configuración inválida.
 *
 * Contrato para el frontend en docs/logica-negocio/crd/API-BANDAS-PRODUCTO.md.
 */
@Path("bndp")
public class BandaProductoRest {

    @EJB
    private BandaProductoDaoService bandaProductoDaoService;

    @EJB
    private BandaProductoService bandaProductoService;

    @Context
    private UriInfo context;

    public BandaProductoRest() {
    }

    /**
     * GET - Todas las bandas, como entidades.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - BANDA_PRODUCTO");
        try {
            List<BandaProducto> lista = bandaProductoDaoService
                    .selectAll(NombreEntidadesCredito.BANDA_PRODUCTO);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener bandas: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Una banda por código.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - BANDA_PRODUCTO - id: " + id);
        try {
            BandaProducto entidad = bandaProductoDaoService
                    .selectById(id, NombreEntidadesCredito.BANDA_PRODUCTO);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("BandaProducto con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(entidad)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la banda: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Bandas activas de una configuración, con el rango en días ya derivado.
     * Es la lectura útil para la pantalla cuando ya tiene el código de la configuración.
     */
    @GET
    @Path("/getByConfiguracion/{idConfiguracion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByConfiguracion(@PathParam("idConfiguracion") Long idConfiguracion) {
        System.out.println("LLEGA AL SERVICIO GET BY CONFIGURACION - BANDA_PRODUCTO"
                + " - configuracion: " + idConfiguracion);
        try {
            return Response.status(Response.Status.OK)
                    .entity(bandaProductoService.selectDetalleByConfiguracion(idConfiguracion))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las bandas de la configuracion: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Crea una banda suelta. Ver la advertencia de la clase.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(BandaProducto registro) {
        System.out.println("LLEGA AL SERVICIO POST - BANDA_PRODUCTO");
        try {
            BandaProducto resultado = bandaProductoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear la banda: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * PUT - Actualiza una banda suelta. Ver la advertencia de la clase.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(BandaProducto registro) {
        System.out.println("LLEGA AL SERVICIO PUT - BANDA_PRODUCTO");
        try {
            BandaProducto resultado = bandaProductoService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar la banda: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * DELETE - Elimina una banda. Ver la advertencia de la clase: dejar una configuración
     * sin banda abierta o con números salteados la vuelve inválida para la clasificación.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - BANDA_PRODUCTO - id: " + id);
        try {
            BandaProducto elimina = new BandaProducto();
            bandaProductoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar la banda: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Búsqueda por criterios dinámicos.
     */
    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de BANDA_PRODUCTO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(bandaProductoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
