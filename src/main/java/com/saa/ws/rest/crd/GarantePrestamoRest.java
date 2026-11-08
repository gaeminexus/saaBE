package com.saa.ws.rest.crd;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.basico.util.UsuarioInvalidoException;
import com.saa.ejb.crd.dao.GarantePrestamoDaoService;
import com.saa.ejb.crd.service.GarantePrestamoService;
import com.saa.model.crd.GarantePrestamo;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Garante del préstamo (CRD.GRPR). POST/PUT reciben {@code ?usuario=} (quien registra, tiene que existir) y los errores de
 * regla de negocio salen 422 con {@code { exito:false, etapa:"VALIDACION", mensaje, codigo }}; un usuario inválido, 400.
 */
@Path("grpr")
public class GarantePrestamoRest {

    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    @EJB
    private GarantePrestamoDaoService garantePrestamoDaoService;

    @EJB
    private GarantePrestamoService garantePrestamoService;

    @Context
    private UriInfo context;

    public GarantePrestamoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET - GRPR getAll");
        try {
            List<GarantePrestamo> lista = garantePrestamoDaoService.selectAll(NombreEntidadesCredito.GARANTE_PRESTAMO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener garantes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET - GRPR getId: " + id);
        try {
            GarantePrestamo garante = garantePrestamoDaoService.selectById(id, NombreEntidadesCredito.GARANTE_PRESTAMO);
            if (garante == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Garante con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(garante).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el garante: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** El garante ACTIVO del préstamo: 200 con él, o 204 si no tiene. */
    @GET
    @Path("/prestamo/{idPrestamo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porPrestamo(@PathParam("idPrestamo") Long idPrestamo) {
        System.out.println("LLEGA AL SERVICIO GET - GRPR prestamo: " + idPrestamo);
        try {
            GarantePrestamo garante = garantePrestamoService.selectActivoByPrestamo(idPrestamo);
            if (garante == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            return Response.status(Response.Status.OK).entity(garante).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el garante del préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(GarantePrestamo registro, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO PUT - GRPR");
        try {
            GarantePrestamo resultado = garantePrestamoService.guardarGarante(registro, usuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al actualizar el garante: ");
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(GarantePrestamo registro, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO POST - GRPR");
        try {
            GarantePrestamo resultado = garantePrestamoService.guardarGarante(registro, usuario);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al crear el garante: ");
        }
    }

    /** Anula el garante (GRPRESTD = 0); no lo borra. */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO DELETE - GRPR id: " + id);
        try {
            GarantePrestamo anulado = garantePrestamoService.anularGarante(id, usuario);
            return Response.status(Response.Status.OK).entity(anulado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al anular el garante: ");
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de GRPR");
        try {
            return Response.status(Response.Status.OK)
                    .entity(garantePrestamoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Response error(Throwable e, String prefijo) {
        if (e instanceof UsuarioInvalidoException) {
            return fallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "No se puede procesar sin un usuario válido: " + ((UsuarioInvalidoException) e).getMotivo(),
                "USUARIO_INVALIDO");
        }
        if (e instanceof IncomeException) {
            String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
            String codigo = mensaje.contains(":") ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";
            return fallo(HTTP_REGLA_DE_NEGOCIO, mensaje, codigo.matches("[A-Z_]+") ? codigo : "");
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(prefijo + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
    }

    private Response fallo(int status, String mensaje, String codigo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("etapa", "VALIDACION");
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("codigo", codigo);
        return Response.status(status).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }
}
