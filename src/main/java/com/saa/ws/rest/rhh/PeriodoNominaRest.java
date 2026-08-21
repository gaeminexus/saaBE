package com.saa.ws.rest.rhh;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.service.ContabilizacionNominaService;
import com.saa.ejb.rhh.service.PeriodoNominaService;
import com.saa.ejb.rhh.service.ProcesoNominaService;
import com.saa.model.cnt.Asiento;
import com.saa.model.rhh.LineaAsientoNomina;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.ResultadoCalculoNomina;
import com.saa.model.rhh.ResultadoCalculoPeriodo;

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

@Path("prdn")
public class PeriodoNominaRest {

    @EJB
    private PeriodoNominaDaoService PeriodoNominaDaoService;

    @EJB
    private PeriodoNominaService PeriodoNominaService;

    @EJB
    private ProcesoNominaService procesoNominaService;

    @EJB
    private ContabilizacionNominaService contabilizacionNominaService;

    @Context
    private UriInfo context;

    public PeriodoNominaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PeriodoNomina> lista = PeriodoNominaDaoService.selectAll(NombreEntidadesRhh.PERIODO_NOMINA);
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
            PeriodoNomina registro = PeriodoNominaDaoService.selectById(id, NombreEntidadesRhh.PERIODO_NOMINA);
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
    public Response put(PeriodoNomina registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PERIODO_NOMINA");
        try {
            PeriodoNomina actualizado = PeriodoNominaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PeriodoNomina registro) {
        System.out.println("LLEGA AL SERVICIO POST - PERIODO_NOMINA");
        try {
            PeriodoNomina creado = PeriodoNominaService.saveSingle(registro);
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
        System.out.println("selectByCriteria de PERIODO_NOMINA");
        try {
            List<PeriodoNomina> lista = PeriodoNominaService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en búsqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)	
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PERIODO_NOMINA");
        try {
            PeriodoNomina elimina = new PeriodoNomina();
            PeriodoNominaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Endpoints de proceso del motor de nomina
    // =====================================================================

    /**
     * Comprueba las precondiciones del periodo. Lista vacia significa que se puede calcular.
     */
    @POST
    @Path("/validar/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validar(@PathParam("idPeriodo") Long idPeriodo) {
        System.out.println("LLEGA AL SERVICIO VALIDAR - PERIODONOMINA, periodo: " + idPeriodo);
        try {
            List<String> mensajes = procesoNominaService.validarPeriodo(idPeriodo);
            return Response.status(Response.Status.OK).entity(mensajes).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al validar el periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Calcula todo el periodo. Es idempotente: se puede repetir.
     */
    @POST
    @Path("/calcular/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response calcular(@PathParam("idPeriodo") Long idPeriodo,
            @QueryParam("usuarioRegistro") String usuario) {
        System.out.println("LLEGA AL SERVICIO CALCULAR - PERIODONOMINA, periodo: " + idPeriodo);
        try {
            ResultadoCalculoPeriodo resultado = procesoNominaService.calcularPeriodo(idPeriodo, usuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al calcular el periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recalcula un solo empleado del periodo.
     */
    @POST
    @Path("/recalcularEmpleado")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response recalcularEmpleado(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO RECALCULAR EMPLEADO - PERIODONOMINA");
        try {
            Long idPeriodo = leeLong(datos, "idPeriodo");
            Long idEmpleado = leeLong(datos, "idEmpleado");
            boolean preservar = leeBoolean(datos, "preservarManuales");
            String usuario = leeTexto(datos, "usuarioRegistro");
            ResultadoCalculoNomina resultado = procesoNominaService.recalcularEmpleado(
                    idPeriodo, idEmpleado, preservar, usuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al recalcular el empleado: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Simula el calculo de un contrato sin persistir nada.
     */
    @POST
    @Path("/simular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response simular(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO SIMULAR - PERIODONOMINA");
        try {
            Long idContrato = leeLong(datos, "idContrato");
            Long idPeriodo = leeLong(datos, "idPeriodo");
            ResultadoCalculoNomina resultado = procesoNominaService.simular(idContrato, idPeriodo);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al simular: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Aprueba el periodo.
     */
    @POST
    @Path("/aprobar/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response aprobar(@PathParam("idPeriodo") Long idPeriodo,
            @QueryParam("usuarioRegistro") String usuario) {
        System.out.println("LLEGA AL SERVICIO APROBAR - PERIODONOMINA, periodo: " + idPeriodo);
        try {
            procesoNominaService.aprobarPeriodo(idPeriodo, usuario);
            return Response.status(Response.Status.OK).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al aprobar el periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Reabre el periodo y retira los acumulados que hubiera escrito el cierre.
     */
    @POST
    @Path("/reabrir/{idPeriodo}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reabrir(@PathParam("idPeriodo") Long idPeriodo, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO REABRIR - PERIODONOMINA, periodo: " + idPeriodo);
        try {
            procesoNominaService.reabrirPeriodo(idPeriodo, leeTexto(datos, "motivo"),
                    leeTexto(datos, "usuarioRegistro"));
            return Response.status(Response.Status.OK).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al reabrir el periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Cierra el periodo y escribe los acumulados ACMN.
     */
    @POST
    @Path("/cerrar/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cerrar(@PathParam("idPeriodo") Long idPeriodo,
            @QueryParam("usuarioRegistro") String usuario) {
        System.out.println("LLEGA AL SERVICIO CERRAR - PERIODONOMINA, periodo: " + idPeriodo);
        try {
            procesoNominaService.cerrarPeriodo(idPeriodo, usuario);
            return Response.status(Response.Status.OK).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al cerrar el periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Excluye a un empleado del periodo.
     */
    @POST
    @Path("/excluirEmpleado")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response excluirEmpleado(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO EXCLUIR EMPLEADO - PERIODONOMINA");
        try {
            procesoNominaService.excluirEmpleado(leeLong(datos, "idPeriodo"),
                    leeLong(datos, "idEmpleado"), leeTexto(datos, "motivo"),
                    leeTexto(datos, "usuarioRegistro"));
            return Response.status(Response.Status.OK).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al excluir el empleado: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
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

    private boolean leeBoolean(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null && Boolean.parseBoolean(valor.toString());
    }

    /**
     * Contabiliza el rol del periodo. En modo historico no genera asiento y devuelve
     * cuerpo vacio, pero el periodo avanza igual a CONTABILIZADO, que es lo que permite
     * cerrarlo despues y escribir los acumulados.
     */
    @POST
    @Path("/contabilizar/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contabilizar(@PathParam("idPeriodo") Long idPeriodo,
            @QueryParam("usuarioRegistro") String usuario) {
        System.out.println("LLEGA AL SERVICIO CONTABILIZAR - PERIODONOMINA, periodo: " + idPeriodo);
        try {
            Asiento asiento = contabilizacionNominaService.contabilizarRol(idPeriodo, usuario);
            if (asiento == null) {
                // Periodo historico: no hay asiento que devolver, pero la operacion tuvo exito.
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            return Response.status(Response.Status.OK).entity(asiento).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al contabilizar el periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    /**
     * Contabiliza las provisiones del periodo. Es un asiento distinto del rol y se guarda en
     * su propia columna. Devuelve cuerpo vacio si el periodo es historico o si el periodo no
     * genero ninguna provision.
     */
    @POST
    @Path("/contabilizarProvisiones/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response contabilizarProvisiones(@PathParam("idPeriodo") Long idPeriodo,
            @QueryParam("usuarioRegistro") String usuario) {
        System.out.println("LLEGA AL SERVICIO CONTABILIZAR PROVISIONES - PERIODONOMINA, periodo: " + idPeriodo);
        try {
            Asiento asiento = contabilizacionNominaService.contabilizarProvisiones(idPeriodo, usuario);
            if (asiento == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            return Response.status(Response.Status.OK).entity(asiento).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al contabilizar las provisiones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Devuelve las lineas que tendria el asiento sin emitirlo. Tipo 1 el rol, 2 las
     * provisiones. Funciona tambien en modo historico.
     */
    @GET
    @Path("/previsualizarAsiento/{idPeriodo}/{tipo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response previsualizarAsiento(@PathParam("idPeriodo") Long idPeriodo,
            @PathParam("tipo") Long tipo) {
        System.out.println("LLEGA AL SERVICIO PREVISUALIZAR ASIENTO - PERIODONOMINA, periodo: "
                + idPeriodo + ", tipo: " + tipo);
        try {
            List<LineaAsientoNomina> lineas = contabilizacionNominaService.previsualizar(idPeriodo, tipo);
            return Response.status(Response.Status.OK).entity(lineas).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al previsualizar el asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
