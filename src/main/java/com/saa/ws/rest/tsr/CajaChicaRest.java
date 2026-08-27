package com.saa.ws.rest.tsr;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.CajaChicaDaoService;
import com.saa.ejb.tsr.service.CajaChicaService;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CajaChica;
import com.saa.model.tsr.NombreEntidadesTesoreria;

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
 * REST para CajaChica (TSR.CJCH). Base path: /cjch
 *
 * Endpoints principales:
 *   POST /cjch/registrar        → registra la caja (con migración opcional de saldo)
 *   GET  /cjch/saldo/{id}       → saldo, alerta y sugerencia de reposición de una caja
 *   GET  /cjch/saldos/{idEmpresa} → saldo de todas las cajas activas de la empresa
 *   GET  /cjch/activas/{idEmpresa} → cajas activas (sin saldo, para selectores)
 */
@Path("cjch")
public class CajaChicaRest {

    @EJB
    private CajaChicaDaoService cajaChicaDaoService;

    @EJB
    private CajaChicaService cajaChicaService;

    @Context
    private UriInfo context;

    public CajaChicaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CajaChica> lista = cajaChicaDaoService.selectAll(NombreEntidadesTesoreria.CAJA_CHICA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cajas chicas: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CajaChica caja = cajaChicaDaoService.selectById(id, NombreEntidadesTesoreria.CAJA_CHICA);
            if (caja == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Caja chica con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(caja).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CajaChica registro) {
        System.out.println("LLEGA AL SERVICIO PUT CAJA_CHICA");
        try {
            CajaChica resultado = cajaChicaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar la caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CajaChica registro) {
        System.out.println("LLEGA AL SERVICIO POST CAJA_CHICA");
        try {
            CajaChica resultado = cajaChicaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear la caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra una caja chica nueva, con migración opcional de saldo desde
     * una cuenta bancaria legada.
     * Body esperado:
     * {
     *   "idEmpresa": 1,
     *   "nombre": "Caja Matriz",
     *   "idPlanCuenta": 10029,
     *   "montoFondo": 500.00,
     *   "montoMaximoGasto": 50.00,       (opcional)
     *   "porcentajeAlerta": 20,          (opcional, default 20)
     *   "responsable": "Juan Pérez",     (opcional)
     *   "idCustodio": 7,                 (opcional)
     *   "observacion": "...",            (opcional)
     *   "saldoInicialMigrado": 350.00,   (opcional: migración desde cuenta bancaria legada)
     *   "idUsuario": 5
     * }
     */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /cjch/registrar");
        try {
            Long idEmpresa = toLong(datos.get("idEmpresa"));
            String nombre = (String) datos.get("nombre");
            Long idPlanCuenta = toLong(datos.get("idPlanCuenta"));
            Double montoFondo = toDouble(datos.get("montoFondo"));
            Double montoMaximoGasto = toDouble(datos.get("montoMaximoGasto"));
            Double porcentajeAlerta = toDouble(datos.get("porcentajeAlerta"));
            String responsable = (String) datos.get("responsable");
            Long idCustodio = toLong(datos.get("idCustodio"));
            String observacion = (String) datos.get("observacion");
            Double saldoInicialMigrado = toDouble(datos.get("saldoInicialMigrado"));
            Long idUsuario = toLong(datos.get("idUsuario"));

            if (idEmpresa == null || nombre == null || idPlanCuenta == null || montoFondo == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idEmpresa, nombre, idPlanCuenta y montoFondo.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Empresa empresa = new Empresa();
            empresa.setCodigo(idEmpresa);
            PlanCuenta planCuenta = new PlanCuenta();
            planCuenta.setCodigo(idPlanCuenta);

            CajaChica caja = new CajaChica();
            caja.setEmpresa(empresa);
            caja.setNombre(nombre);
            caja.setPlanCuenta(planCuenta);
            caja.setMontoFondo(montoFondo);
            caja.setMontoMaximoGasto(montoMaximoGasto);
            caja.setPorcentajeAlerta(porcentajeAlerta);
            caja.setResponsable(responsable);
            if (idCustodio != null) {
                Usuario custodio = new Usuario();
                custodio.setCodigo(idCustodio);
                caja.setCustodio(custodio);
            }
            caja.setObservacion(observacion);

            CajaChica resultado = cajaChicaService.registrar(caja, saldoInicialMigrado, idUsuario);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar la caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Saldo, alerta y sugerencia de reposición de una caja chica.
     */
    @GET
    @Path("/saldo/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saldo(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET /cjch/saldo/" + id);
        try {
            Map<String, Object> resultado = cajaChicaService.saldo(id);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el saldo de la caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Saldo de todas las cajas chicas activas de una empresa (para la alerta global).
     */
    @GET
    @Path("/saldos/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saldos(@PathParam("idEmpresa") Long idEmpresa) {
        System.out.println("LLEGA AL SERVICIO GET /cjch/saldos/" + idEmpresa);
        try {
            List<Map<String, Object>> resultado = cajaChicaService.saldos(idEmpresa);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los saldos de las cajas chicas: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Cajas chicas activas de una empresa (sin saldo, para selectores).
     */
    @GET
    @Path("/activas/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activas(@PathParam("idEmpresa") Long idEmpresa) {
        System.out.println("LLEGA AL SERVICIO GET /cjch/activas/" + idEmpresa);
        try {
            List<CajaChica> resultado = cajaChicaService.activas(idEmpresa);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las cajas chicas activas: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(cajaChicaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CAJA_CHICA");
        try {
            CajaChica elimina = new CajaChica();
            cajaChicaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar la caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Long toLong(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).longValue();
        try {
            return Long.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Double toDouble(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).doubleValue();
        try {
            return Double.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
}
