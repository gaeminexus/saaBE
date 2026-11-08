package com.saa.ws.rest.crd;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.basico.util.UsuarioInvalidoException;
import com.saa.ejb.crd.dao.ReglaAprobacionPrestamoDaoService;
import com.saa.ejb.crd.service.ReglaAprobacionPrestamoService;
import com.saa.model.crd.ReglaAprobacionPrestamo;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Configuración de niveles de aprobación de préstamos: CRD.RGAP (DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md,
 * B4). POST/PUT reciben {@code ?usuario=} (quien configura; tiene que existir). DELETE deja el registro inactivo, no lo borra.
 * Los errores de regla salen 422 con {@code { exito:false, etapa:"VALIDACION", mensaje, codigo }}; usuario inválido, 400.
 */
@Path("rgap")
public class ReglaAprobacionPrestamoRest {

    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    @EJB
    private ReglaAprobacionPrestamoDaoService reglaAprobacionPrestamoDaoService;

    @EJB
    private ReglaAprobacionPrestamoService reglaAprobacionPrestamoService;

    public ReglaAprobacionPrestamoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET - RGAP getAll");
        try {
            List<ReglaAprobacionPrestamo> lista = reglaAprobacionPrestamoDaoService.selectAll(NombreEntidadesCredito.REGLA_APROBACION_PRESTAMO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al obtener ReglaAprobacionPrestamo: ");
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET - RGAP getId: " + id);
        try {
            ReglaAprobacionPrestamo registro = reglaAprobacionPrestamoDaoService.selectById(id, NombreEntidadesCredito.REGLA_APROBACION_PRESTAMO);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("ReglaAprobacionPrestamo con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al obtener ReglaAprobacionPrestamo: ");
        }
    }

    @EJB
    private com.saa.ejb.crd.service.NivelAprobacionService nivelAprobacionService;

    /** La regla con sus niveles y los usuarios de cada nivel (pantalla de configuración). */
    @GET
    @Path("/completa/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response completa(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET - RGAP completa: " + id);
        try {
            return Response.status(Response.Status.OK).entity(nivelAprobacionService.reglaCompleta(id))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al obtener la regla completa: ");
        }
    }

    /** Qué regla aplicaría a un tipo, monto y plazo, con sus niveles; sirve para probar la configuración. */
    @GET
    @Path("/simular")
    @Produces(MediaType.APPLICATION_JSON)
    public Response simular(@QueryParam("idTipo") Long idTipo, @QueryParam("monto") Double monto,
            @QueryParam("plazo") Long plazo) {
        System.out.println("LLEGA AL SERVICIO GET - RGAP simular - tipo: " + idTipo + " monto: " + monto + " plazo: " + plazo);
        try {
            return Response.status(Response.Status.OK).entity(nivelAprobacionService.simular(idTipo, monto, plazo))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al simular la regla: ");
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ReglaAprobacionPrestamo registro, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO PUT - RGAP");
        try {
            ReglaAprobacionPrestamo resultado = reglaAprobacionPrestamoService.guardar(registro, usuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al actualizar ReglaAprobacionPrestamo: ");
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ReglaAprobacionPrestamo registro, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO POST - RGAP");
        try {
            ReglaAprobacionPrestamo resultado = reglaAprobacionPrestamoService.guardar(registro, usuario);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al crear ReglaAprobacionPrestamo: ");
        }
    }

    /** Lo deja inactivo (estado 0); no lo borra. */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO DELETE - RGAP id: " + id);
        try {
            ReglaAprobacionPrestamo inactivo = reglaAprobacionPrestamoService.inactivar(id, usuario);
            return Response.status(Response.Status.OK).entity(inactivo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al inactivar ReglaAprobacionPrestamo: ");
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de RGAP");
        try {
            return Response.status(Response.Status.OK).entity(reglaAprobacionPrestamoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Response error(Throwable e, String prefijo) {
        if (e instanceof UsuarioInvalidoException) {
            return fallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "No se puede procesar sin un usuario válido: " + ((UsuarioInvalidoException) e).getMotivo(), "USUARIO_INVALIDO");
        }
        if (e instanceof IncomeException) {
            String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
            String codigo = mensaje.contains(":") ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";
            return fallo(HTTP_REGLA_DE_NEGOCIO, mensaje, codigo.matches("[A-Z_]+") ? codigo : "");
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(prefijo + e.getMessage())
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
