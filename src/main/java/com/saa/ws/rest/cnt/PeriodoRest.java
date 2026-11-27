package com.saa.ws.rest.cnt;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.PeriodoDaoService;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.Periodo;

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

@Path("prdo")
public class PeriodoRest {

    @EJB
    private PeriodoDaoService periodoDaoService;

    @EJB
    private PeriodoService periodoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public PeriodoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of PeriodoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Periodo> lista = periodoDaoService.selectAll(NombreEntidadesContabilidad.PERIODO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener periodos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Periodo periodo = periodoDaoService.selectById(id, NombreEntidadesContabilidad.PERIODO);
            if (periodo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Periodo con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(periodo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Path("/verificaPeriodoAbierto/{idEmpresa}/{fecha}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificaPeriodoAbierto(@PathParam("idEmpresa") Long idEmpresa, @PathParam("fecha") String fecha) {
        try {
            LocalDate localDate = LocalDate.parse(fecha);
            Periodo periodo = periodoService.verificaPeriodoAbierto(idEmpresa, localDate);
            return Response.status(Response.Status.OK).entity(periodo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al verificar periodo abierto: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // C17 — Cierres bloqueantes (docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md)
    // =====================================================================

    @EJB
    private com.saa.ejb.cnt.service.EstadoCierreService estadoCierreService;

    @EJB
    private com.saa.ejb.cnt.service.CierrePeriodoContableService cierrePeriodoContableService;

    /**
     * GET /prdo/estado/{idEmpresa}/{anio}/{mes} — estado de cierre del mes: contable (ABIERTO/CERRADO, que
     * decide estados financieros DEFINITIVOS o PRELIMINARES) y de cartera, con fecha y usuario de cierre.
     */
    @GET
    @Path("/estado/{idEmpresa}/{anio}/{mes}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response estado(@PathParam("idEmpresa") Long idEmpresa, @PathParam("anio") Long anio,
            @PathParam("mes") Long mes) {
        try {
            return Response.ok(estadoCierreService.estadoPeriodo(idEmpresa, anio, mes), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaCierre(e, "Error al consultar el estado del período: ");
        }
    }

    /**
     * POST /prdo/cerrar?idEmpresa=&anio=&mes=&usuario=&ip=&motivo= — cierre contable del mes. Exige período
     * mayorizado, cartera del mes cerrada y orden (ver el diseño). Auditado en CNT.HCPR.
     * C51: cuerpo OPCIONAL (JSON): lista {codigoControl, motivo} de excepciones al candado (saa.cierres.candadoCierre); sin cuerpo, como siempre.
     */
    @POST
    @Path("/cerrar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cerrar(@jakarta.ws.rs.QueryParam("idEmpresa") Long idEmpresa,
            @jakarta.ws.rs.QueryParam("anio") Long anio, @jakarta.ws.rs.QueryParam("mes") Long mes,
            @jakarta.ws.rs.QueryParam("usuario") String usuario, @jakarta.ws.rs.QueryParam("ip") String ip,
            @jakarta.ws.rs.QueryParam("motivo") String motivo,
            String cuerpo) {
        System.out.println("LLEGA AL SERVICIO CERRAR - PERIODO " + anio + "-" + mes + " empresa " + idEmpresa);
        try {
            // C51: el cuerpo es opcional. Se recibe como texto (sin @Consumes y sin depender del Content-Type: el FE hace post(url, null) sin cabecera) y se lee
            // como lista JSON solo si trae algo; vacio = sin excepciones.
            java.util.List<com.saa.ejb.cnt.service.dto.ExcepcionCandado> excepciones = excepcionesDelCuerpo(cuerpo);
            return Response.ok(cierrePeriodoContableService.cerrar(idEmpresa, anio, mes, usuario, ip, motivo, excepciones),
                    MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.UsuarioInvalidoException eUsuario) {
            // U-2: usuario faltante o inexistente -> 400; no se procesó nada.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No se puede procesar sin un usuario válido: " + eUsuario.getMotivo())
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaCierre(e, "Error al cerrar el período contable: ");
        }
    }

    /**
     * POST /prdo/reabrir?idEmpresa=&anio=&mes=&usuario=&ip=&motivo= — reapertura del período contable.
     * Solo el último cerrado (cascada desde el más reciente). Motivo obligatorio. Auditado en CNT.HCPR.
     */
    @POST
    @Path("/reabrir")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reabrir(@jakarta.ws.rs.QueryParam("idEmpresa") Long idEmpresa,
            @jakarta.ws.rs.QueryParam("anio") Long anio, @jakarta.ws.rs.QueryParam("mes") Long mes,
            @jakarta.ws.rs.QueryParam("usuario") String usuario, @jakarta.ws.rs.QueryParam("ip") String ip,
            @jakarta.ws.rs.QueryParam("motivo") String motivo) {
        System.out.println("LLEGA AL SERVICIO REABRIR - PERIODO " + anio + "-" + mes + " empresa " + idEmpresa);
        try {
            return Response.ok(cierrePeriodoContableService.reabrir(idEmpresa, anio, mes, usuario, ip, motivo),
                    MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.UsuarioInvalidoException eUsuario) {
            // U-2: usuario faltante o inexistente -> 400; no se procesó nada.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No se puede procesar sin un usuario válido: " + eUsuario.getMotivo())
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaCierre(e, "Error al reabrir el período contable: ");
        }
    }

    /**
     * POST /prdo/desmayorizar?idEmpresa=&anio=&mes=&usuario=&ip=&motivo= — desmayoriza un período MAYORIZADO sin cierre
     * contable (6.5 del cuadre, DS-21), incluso con una referencia PRDOMYRZ colgante. Motivo obligatorio. Auditado en
     * CNT.HCPR (acción DESMAYORIZAR). Contrato: docs/logica-negocio/cnt/API-DESMAYORIZAR-PERIODO.md.
     */
    @POST
    @Path("/desmayorizar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response desmayorizar(@jakarta.ws.rs.QueryParam("idEmpresa") Long idEmpresa,
            @jakarta.ws.rs.QueryParam("anio") Long anio, @jakarta.ws.rs.QueryParam("mes") Long mes,
            @jakarta.ws.rs.QueryParam("usuario") String usuario, @jakarta.ws.rs.QueryParam("ip") String ip,
            @jakarta.ws.rs.QueryParam("motivo") String motivo) {
        System.out.println("LLEGA AL SERVICIO DESMAYORIZAR - PERIODO " + anio + "-" + mes + " empresa " + idEmpresa);
        try {
            return Response.ok(cierrePeriodoContableService.desmayorizar(idEmpresa, anio, mes, usuario, ip, motivo),
                    MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.UsuarioInvalidoException eUsuario) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No se puede procesar sin un usuario válido: " + eUsuario.getMotivo())
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaCierre(e, "Error al desmayorizar el período contable: ");
        }
    }

    /**
     * GET /prdo/bitacora/{idEmpresa}?tipo=CONTABLE|CARTERA — bitácora de cierres y reaperturas (CNT.HCPR) de la
     * empresa, de la más reciente a la más antigua. Solo lectura (la muestra la pantalla de cierre de períodos
     * del frontend). {@code tipo} es opcional; sin él devuelve contable y cartera.
     */
    @GET
    @Path("/bitacora/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response bitacora(@PathParam("idEmpresa") Long idEmpresa, @jakarta.ws.rs.QueryParam("tipo") String tipo) {
        try {
            return Response.ok(cierrePeriodoContableService.listarBitacora(idEmpresa, tipo), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaCierre(e, "Error al consultar la bitácora de cierres: ");
        }
    }

    @EJB
    private com.saa.ejb.cnt.service.ControlCierreService controlCierreService;

    /**
     * GET /prdo/controlCierre/{idEmpresa}/{anio}/{mes} — semáforo de controles de cierre del mes (pase a
     * producción B-1..B-3): cuentas puente D-024, ecuación contable, cartera vs contabilidad por grupo, bancos
     * vs extracto, asientos descuadrados o en borrador y cuadratura B17 ↔ G. Lista de
     * {grupo, codigo, descripcion, valor, esperado, diferencia, estado OK|ADVERTENCIA|ERROR, detalle}. Solo
     * lectura; no bloquea el cierre. docs/logica-negocio/cnt/CONTROL-CIERRE-MES.md.
     */
    @GET
    @Path("/controlCierre/{idEmpresa}/{anio}/{mes}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response controlCierre(@PathParam("idEmpresa") Long idEmpresa, @PathParam("anio") Long anio,
            @PathParam("mes") Long mes) {
        try {
            return Response.ok(controlCierreService.controlar(idEmpresa, anio, mes), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaCierre(e, "Error al consultar los controles de cierre: ");
        }
    }

    /**
     * C51: GET /prdo/candado/{idEmpresa}/{anio}/{mes} — que bloquearia hoy el cierre contable del mes (controles del semaforo en ERROR y las G40-G51:
     * completa, validacion sin errores, vigente segun el C50). Solo lectura; no depende de que el candado este encendido.
     */
    @GET
    @Path("/candado/{idEmpresa}/{anio}/{mes}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response candado(@PathParam("idEmpresa") Long idEmpresa, @PathParam("anio") Long anio, @PathParam("mes") Long mes) {
        try {
            return Response.ok(candadoCierreContableService.consultar(idEmpresa, anio, mes, null), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaCierre(e, "Error al consultar el candado del cierre: ");
        }
    }

    @EJB
    private com.saa.ejb.cnt.service.CandadoCierreContableService candadoCierreContableService;

    private static java.util.List<com.saa.ejb.cnt.service.dto.ExcepcionCandado> excepcionesDelCuerpo(String cuerpo) throws Throwable {
        if (cuerpo == null || cuerpo.trim().isEmpty()) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(cuerpo.trim(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.saa.ejb.cnt.service.dto.ExcepcionCandado>>() { });
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new com.saa.basico.util.IncomeException("CIERRE_CANDADO_EXCEPCION: el cuerpo no es una lista JSON de {codigoControl, motivo}: " + e.getOriginalMessage());
        }
    }

    /**
     * C52: GET /prdo/saldosContrarios/{idEmpresa}/{anio}/{mes} — cuentas de movimiento con saldo contrario a su naturaleza al corte del mes, con el asiento
     * sugerido (no se graba). Solo lectura.
     */
    @GET
    @Path("/saldosContrarios/{idEmpresa}/{anio}/{mes}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saldosContrarios(@PathParam("idEmpresa") Long idEmpresa, @PathParam("anio") Long anio, @PathParam("mes") Long mes) {
        try {
            return Response.ok(saldoContrarioCuentaService.saldosContrarios(idEmpresa, anio, mes), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaCierre(e, "Error al consultar los saldos contrarios: ");
        }
    }

    /**
     * C52: POST /prdo/regularizarRedondeos/{idEmpresa}/{anio}/{mes}?usuario=&simular=true|false — unica correccion automatica: las cuentas de redondeo de la
     * plantilla con saldo acreedor van a otros ingresos (asiento del mes, fecha del ultimo dia, idempotente). Con el periodo cerrado no. Por defecto simula.
     */
    @POST
    @Path("/regularizarRedondeos/{idEmpresa}/{anio}/{mes}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response regularizarRedondeos(@PathParam("idEmpresa") Long idEmpresa, @PathParam("anio") Long anio, @PathParam("mes") Long mes,
            @jakarta.ws.rs.QueryParam("usuario") String usuario, @jakarta.ws.rs.QueryParam("simular") Boolean simular) {
        System.out.println("LLEGA AL SERVICIO REGULARIZAR REDONDEOS - PERIODO " + anio + "-" + mes + " empresa " + idEmpresa);
        try {
            return Response.ok(saldoContrarioCuentaService.regularizarRedondeos(idEmpresa, anio, mes, usuario, simular == null || simular.booleanValue()),
                    MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.UsuarioInvalidoException eUsuario) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No se puede procesar sin un usuario valido: " + eUsuario.getMotivo()).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaCierre(e, "Error al regularizar los redondeos: ");
        }
    }

    @EJB
    private com.saa.ejb.cnt.service.SaldoContrarioCuentaService saldoContrarioCuentaService;

    /** Errores de negocio de cierre (IncomeException) son 400 con el mensaje; lo inesperado, 500. */
    private Response respuestaCierre(Throwable e, String prefijo) {
        boolean negocio = e instanceof com.saa.basico.util.IncomeException;
        return Response.status(negocio ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR)
                .entity(prefijo + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * PUT method for updating or creating an instance of PeriodoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Periodo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PERIODO");
        try {
            Periodo resultado = periodoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.UsuarioInvalidoException eUsuario) {
            // U-2: usuario faltante o inexistente -> 400; no se procesó nada.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No se puede procesar sin un usuario válido: " + eUsuario.getMotivo())
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of PeriodoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Periodo registro) {
        System.out.println("LLEGA AL SERVICIO POST - PERIODO");
        try {
            Periodo resultado = periodoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.UsuarioInvalidoException eUsuario) {
            // U-2: usuario faltante o inexistente -> 400; no se procesó nada.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No se puede procesar sin un usuario válido: " + eUsuario.getMotivo())
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of PeriodoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de PERIODO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(periodoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
    /**
     * POST method for updating or creating an instance of PeriodoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PERIODO");
        try {
            String resultado = periodoService.remove(id);
            System.out.println("MENSAJE DEVUELTO: " + resultado);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.UsuarioInvalidoException eUsuario) {
            // U-2: usuario faltante o inexistente -> 400; no se procesó nada.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No se puede procesar sin un usuario válido: " + eUsuario.getMotivo())
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar periodo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
