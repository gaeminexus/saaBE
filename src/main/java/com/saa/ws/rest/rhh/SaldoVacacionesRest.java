package com.saa.ws.rest.rhh;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.SaldoVacacionesDaoService;
import com.saa.ejb.rhh.service.AcreditacionVacacionesService;
import com.saa.ejb.rhh.service.SaldoVacacionesService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.SaldoVacaciones;

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

@Path("sldv")
public class SaldoVacacionesRest {

    @EJB
    private SaldoVacacionesDaoService SaldoVacacionesDaoService;

    @EJB
    private SaldoVacacionesService SaldoVacacionesService;

    @EJB
    private AcreditacionVacacionesService acreditacionVacacionesService;

    @Context
    private UriInfo context;

    public SaldoVacacionesRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<SaldoVacaciones> lista = SaldoVacacionesDaoService.selectAll(NombreEntidadesRhh.SALDO_VACACIONES);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            SaldoVacaciones registro = SaldoVacacionesDaoService.selectById(id, NombreEntidadesRhh.SALDO_VACACIONES);
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
    public Response put(SaldoVacaciones registro) {
        System.out.println("LLEGA AL SERVICIO PUT - SALDO_VACACIONES");
        try {
            SaldoVacaciones actualizado = SaldoVacacionesService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(SaldoVacaciones registro) {
        System.out.println("LLEGA AL SERVICIO POST - SALDO_VACACIONES");
        try {
            SaldoVacaciones creado = SaldoVacacionesService.saveSingle(registro);
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
        System.out.println("selectByCriteria de SALDO_VACACIONES");
        try {
            List<SaldoVacaciones> lista = SaldoVacacionesService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en búsqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - SALDO_VACACIONES");
        try {
            SaldoVacaciones elimina = new SaldoVacaciones();
            SaldoVacacionesDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Endpoints de proceso de vacaciones
    // =====================================================================

    /**
     * Acredita el periodo anual de vacaciones a los empleados que cumplieron un anio de
     * servicio. Caduca antes los saldos vencidos y arrastra lo no gozado.
     */
    @POST
    @Path("/acreditar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response acreditar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO ACREDITAR - SALDOVACACIONES");
        try {
            Long idEmpresa = leeLong(datos, "idEmpresa");
            LocalDate fechaCorte = leeFecha(datos, "fechaCorte");
            String usuario = leeTexto(datos, "usuarioRegistro");
            int acreditados = acreditacionVacacionesService.acreditar(idEmpresa, fechaCorte, usuario);
            return Response.status(Response.Status.OK).entity(acreditados).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al acreditar las vacaciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Dias de vacaciones disponibles de un empleado, sumando los periodos no caducados.
     */
    @GET
    @Path("/disponible/{idEmpleado}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disponible(@PathParam("idEmpleado") Long idEmpleado) {
        System.out.println("LLEGA AL SERVICIO DISPONIBLE - SALDOVACACIONES, empleado: " + idEmpleado);
        try {
            Double dias = acreditacionVacacionesService.diasDisponibles(idEmpleado);
            return Response.status(Response.Status.OK).entity(dias).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al consultar los dias disponibles: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Valor del dia de vacaciones de un empleado a una fecha de corte, tal como lo
     * calcula el motor: ventana movil de doce meses sobre PVNM de periodos CERRADOS,
     * ponderada por dias contra el saldo de apertura.
     *
     * <p>Es de SOLO LECTURA --no escribe nada, a diferencia de acreditar-- y existe
     * porque esa tarifa no queda persistida en ninguna parte salvo cuando se acredita
     * un periodo nuevo o se liquida a alguien. Sin este endpoint no hay forma de
     * contrastar la tarifa de un mes contra el calculo a mano.</p>
     *
     * @param idEmpleado	: Codigo del empleado
     * @param fechaCorte	: Fecha de corte en ISO, yyyy-MM-dd
     */
    @GET
    @Path("/valorDia/{idEmpleado}/{fechaCorte}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response valorDia(@PathParam("idEmpleado") Long idEmpleado,
            @PathParam("fechaCorte") String fechaCorte) {
        System.out.println("LLEGA AL SERVICIO VALORDIA - SALDOVACACIONES, empleado: " + idEmpleado
                + ", corte: " + fechaCorte);
        try {
            Double valor = acreditacionVacacionesService.valorDiaVacaciones(idEmpleado,
                    LocalDate.parse(fechaCorte));
            return Response.status(Response.Status.OK).entity(valor).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al consultar el valor del dia de vacaciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Marca como caducados los saldos que superaron el plazo legal de acumulacion.
     * Devuelve el detalle de lo caducado para que quede constancia.
     */
    @POST
    @Path("/caducar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response caducar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO CADUCAR - SALDOVACACIONES");
        try {
            Long idEmpresa = leeLong(datos, "idEmpresa");
            LocalDate fechaCorte = leeFecha(datos, "fechaCorte");
            String usuario = leeTexto(datos, "usuarioRegistro");
            List<String> avisos = acreditacionVacacionesService.caducarSaldos(idEmpresa, fechaCorte, usuario);
            return Response.status(Response.Status.OK).entity(avisos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al caducar los saldos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
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
