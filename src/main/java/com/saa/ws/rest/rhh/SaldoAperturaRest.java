package com.saa.ws.rest.rhh;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.SaldoAperturaDaoService;
import com.saa.ejb.rhh.service.MigracionRhhService;
import com.saa.ejb.rhh.service.SaldoAperturaService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.SaldoApertura;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
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

@Path("slap")
public class SaldoAperturaRest {

    @EJB
    private SaldoAperturaDaoService saldoAperturaDaoService;

    @EJB
    private SaldoAperturaService saldoAperturaService;

    @EJB
    private MigracionRhhService migracionRhhService;

    @Context
    private UriInfo context;

    public SaldoAperturaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - SALDOAPERTURA");
        try {
            List<SaldoApertura> lista = saldoAperturaDaoService.selectAll(NombreEntidadesRhh.SALDO_APERTURA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - SALDOAPERTURA");
        try {
            SaldoApertura registro = saldoAperturaDaoService.selectById(id, NombreEntidadesRhh.SALDO_APERTURA);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Registro con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(SaldoApertura registro) {
        System.out.println("LLEGA AL SERVICIO PUT - SALDOAPERTURA");
        try {
            SaldoApertura actualizado = saldoAperturaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(SaldoApertura registro) {
        System.out.println("LLEGA AL SERVICIO POST - SALDOAPERTURA");
        try {
            SaldoApertura creado = saldoAperturaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(creado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de SALDOAPERTURA");
        try {
            List<SaldoApertura> lista = saldoAperturaService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - SALDOAPERTURA");
        try {
            saldoAperturaService.remove(List.of(id));
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Endpoints de proceso de la migracion de apertura
    // =====================================================================

    /**
     * Carga el archivo de saldos de apertura. Solo inserta en RHH.SLAP: no
     * materializa nada en las tablas operativas.
     */
    @POST
    @Path("/cargar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargar(@FormParam("archivo") InputStream archivo,
            @FormParam("idEmpresa") String idEmpresaParam,
            @FormParam("fechaCorte") String fechaCorteParam,
            @FormParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO CARGAR - SALDOAPERTURA, empresa: " + idEmpresaParam
                + ", corte: " + fechaCorteParam);
        try {
            if (archivo == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se ha enviado el archivo").type(MediaType.APPLICATION_JSON).build();
            }
            Long idEmpresa = Long.valueOf(idEmpresaParam);
            LocalDate fechaCorte = LocalDate.parse(fechaCorteParam);
            int cargados = migracionRhhService.cargarSaldosApertura(archivo, idEmpresa, fechaCorte, usuarioRegistro);
            return Response.status(Response.Status.OK).entity(cargados).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al cargar los saldos de apertura: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Devuelve las inconsistencias de los saldos cargados. Lista vacia significa
     * que la migracion se puede aplicar.
     */
    @GET
    @Path("/validar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validar(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("fechaCorte") String fechaCorteParam) {
        System.out.println("LLEGA AL SERVICIO VALIDAR - SALDOAPERTURA, empresa: " + idEmpresa
                + ", corte: " + fechaCorteParam);
        try {
            LocalDate fechaCorte = LocalDate.parse(fechaCorteParam);
            List<String> inconsistencias = migracionRhhService.validarSaldosApertura(idEmpresa, fechaCorte);
            return Response.status(Response.Status.OK).entity(inconsistencias).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al validar los saldos de apertura: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Materializa los saldos en las tablas operativas. Es idempotente: un saldo ya
     * aplicado se salta.
     */
    @POST
    @Path("/aplicar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aplicar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO APLICAR - SALDOAPERTURA");
        try {
            Long idEmpresa = leeLong(datos, "idEmpresa");
            LocalDate fechaCorte = leeFecha(datos, "fechaCorte");
            String usuario = leeTexto(datos, "usuarioRegistro");
            int aplicados = migracionRhhService.aplicarSaldosApertura(idEmpresa, fechaCorte, usuario);
            return Response.status(Response.Status.OK).entity(aplicados).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al aplicar los saldos de apertura: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Deshace la materializacion usando SLAPRFTB y SLAPRFID.
     */
    @POST
    @Path("/revertir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response revertir(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO REVERTIR - SALDOAPERTURA");
        try {
            Long idEmpresa = leeLong(datos, "idEmpresa");
            LocalDate fechaCorte = leeFecha(datos, "fechaCorte");
            String usuario = leeTexto(datos, "usuarioRegistro");
            int revertidos = migracionRhhService.revertirSaldosApertura(idEmpresa, fechaCorte, usuario);
            return Response.status(Response.Status.OK).entity(revertidos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al revertir los saldos de apertura: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Long leeLong(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor == null ? null : Long.valueOf(valor.toString());
    }

    private String leeTexto(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor == null ? null : valor.toString();
    }

    private LocalDate leeFecha(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor == null ? null : LocalDate.parse(valor.toString());
    }
}
