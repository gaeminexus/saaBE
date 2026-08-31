package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.ejb.crd.dao.CuentaTipoAporteDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.CuentaTipoAporteService;
import com.saa.basico.ejb.EmpresaDaoService;
import com.saa.ejb.cnt.dao.PlanCuentaDaoService;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.CuentaTipoAporte;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoAporte;
import com.saa.model.scp.Empresa;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Mantenimiento de {@code CRD.CTAP} — cuentas contables por tipo de aporte y empresa. Ver el
 * javadoc de {@link CuentaTipoAporte}.
 *
 * <p>Contrato para el frontend congelado 2026-08-31: cualquier cambio de ruta, request o
 * response se registra en {@code docs/logica-negocio/crd/MAPEO-CUENTAS-TIPO-APORTE.md} en el
 * mismo cambio.
 *
 * <p><b>Sin DELETE</b> — baja lógica con {@code estado = 0} (real: {@code
 * selectByTipoAporteYEmpresa}/{@code selectByEmpresa} la filtran) vía {@code /desactivar/{codigo}}
 * y {@code /activar/{codigo}} — dos verbos separados a propósito, no un solo endpoint con el
 * estado como parámetro: así el frontend no puede armar la URL con el valor equivocado.
 *
 * <p>Las entidades relacionadas (tipoAporte, empresa, cuentaPasivo, cuentaLiquidacion) viajan
 * como objetos JSON con al menos {@code codigo} — no hay capa de DTO, las entidades JPA se
 * serializan directamente (CLAUDE.md §Serialización). En la respuesta de POST/PUT esos objetos
 * anidados vienen tal como los dejó la operación (algunos solo con {@code codigo}), igual que
 * en {@code ConfiguracionBandaProductoRest}; para los datos completos, usar {@code /getAll} o
 * {@code /porEmpresa/{idEmpresa}}.
 *
 * <p>Los errores de esta clase se arman como {@code String} con
 * {@code MediaType.APPLICATION_JSON}, igual que el resto de {@code ws.rest}; el filtro global
 * {@link com.saa.ws.rest.MensajeErrorJsonFilter} los envuelve en {@code {"mensaje": "..."}}
 * antes de que salgan por el cable — no viajan como texto plano.
 */
@Path("ctap")
public class CuentaTipoAporteRest {

    @EJB
    private CuentaTipoAporteDaoService cuentaTipoAporteDaoService;

    @EJB
    private CuentaTipoAporteService cuentaTipoAporteService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private EmpresaDaoService empresaDaoService;

    @EJB
    private PlanCuentaDaoService planCuentaDaoService;

    public CuentaTipoAporteRest() {
    }

    /** GET - Todas las configuraciones (activas e inactivas), como entidades. */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - CUENTA_TIPO_APORTE");
        try {
            List<CuentaTipoAporte> lista = cuentaTipoAporteDaoService
                    .selectAll(NombreEntidadesCredito.CUENTA_TIPO_APORTE);
            return Response.status(Response.Status.OK)
                    .entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las cuentas por tipo de aporte: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** GET - Configuraciones ACTIVAS de una empresa, para el mantenimiento y el diagnóstico. */
    @GET
    @Path("/porEmpresa/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEmpresa(@PathParam("idEmpresa") Long idEmpresa) {
        System.out.println("LLEGA AL SERVICIO POR EMPRESA - CUENTA_TIPO_APORTE - empresa: " + idEmpresa);
        try {
            List<CuentaTipoAporte> lista = cuentaTipoAporteDaoService.selectByEmpresa(idEmpresa);
            return Response.status(Response.Status.OK)
                    .entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las cuentas de la empresa: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - Crea una configuración. Valida ANTES de grabar: los cuatro campos obligatorios,
     * que las tres referencias (tipo de aporte, empresa, las dos cuentas) existan de verdad, y
     * que no exista ya una configuración activa para el mismo (tipo de aporte, empresa) — para
     * no dejar que salte la violación de {@code UK_CTAP_TPAP_PJRQ} como error de Oracle.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CuentaTipoAporte registro) {
        System.out.println("LLEGA AL SERVICIO POST - CUENTA_TIPO_APORTE");
        try {
            if (registro == null) {
                return errorSimple(Response.Status.BAD_REQUEST, "Debe enviar la configuracion a crear");
            }
            if (registro.getCodigo() != null) {
                return errorSimple(Response.Status.BAD_REQUEST,
                        "No se indica codigo al crear; use PUT para editar una configuracion existente");
            }

            Response validacion = validarReferencias(registro);
            if (validacion != null) {
                return validacion;
            }

            Long idTipoAporte = registro.getTipoAporte().getCodigo();
            Long idEmpresa = registro.getEmpresa().getCodigo();

            CuentaTipoAporte existente = cuentaTipoAporteDaoService
                    .selectByTipoAporteYEmpresa(idTipoAporte, idEmpresa);
            if (existente != null) {
                return errorSimple(Response.Status.CONFLICT,
                        "Ya existe una configuracion activa (codigo " + existente.getCodigo()
                                + ") para el tipo de aporte " + idTipoAporte
                                + " y la empresa " + idEmpresa + "; edite esa fila en vez de crear otra");
            }

            registro.setEstado(Long.valueOf(Estado.ACTIVO));

            CuentaTipoAporte creado = cuentaTipoAporteService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(creado).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear la cuenta por tipo de aporte: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT - Edita las DOS CUENTAS de una fila existente. {@code tipoAporte}, {@code empresa} y
     * {@code estado} de la fila NO cambian por esta vía aunque el body los traiga: para
     * reasignar el tipo de aporte o la empresa de una configuración, se da de baja la fila (a
     * futuro) y se crea una nueva — evita que una edición "de cuentas" mueva por accidente la
     * fila a otro (tipo, empresa) y choque con {@code UK_CTAP_TPAP_PJRQ} en silencio.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CuentaTipoAporte registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CUENTA_TIPO_APORTE - codigo: "
                + (registro != null ? registro.getCodigo() : null));
        try {
            if (registro == null || registro.getCodigo() == null) {
                return errorSimple(Response.Status.BAD_REQUEST,
                        "Debe indicar el codigo de la configuracion a editar");
            }

            CuentaTipoAporte existente = cuentaTipoAporteDaoService.find(new CuentaTipoAporte(), registro.getCodigo());
            if (existente == null) {
                return errorSimple(Response.Status.NOT_FOUND,
                        "No existe una configuracion con codigo " + registro.getCodigo());
            }

            if (registro.getCuentaPasivo() == null || registro.getCuentaPasivo().getCodigo() == null) {
                return errorSimple(Response.Status.BAD_REQUEST, "Debe indicar la cuenta de pasivo");
            }
            if (registro.getCuentaLiquidacion() == null || registro.getCuentaLiquidacion().getCodigo() == null) {
                return errorSimple(Response.Status.BAD_REQUEST, "Debe indicar la cuenta de liquidacion");
            }

            Long idCuentaPasivo = registro.getCuentaPasivo().getCodigo();
            PlanCuenta cuentaPasivo = planCuentaDaoService.find(new PlanCuenta(), idCuentaPasivo);
            if (cuentaPasivo == null) {
                return errorSimple(Response.Status.NOT_FOUND, "La cuenta de pasivo " + idCuentaPasivo + " no existe");
            }

            Long idCuentaLiquidacion = registro.getCuentaLiquidacion().getCodigo();
            PlanCuenta cuentaLiquidacion = planCuentaDaoService.find(new PlanCuenta(), idCuentaLiquidacion);
            if (cuentaLiquidacion == null) {
                return errorSimple(Response.Status.NOT_FOUND,
                        "La cuenta de liquidacion " + idCuentaLiquidacion + " no existe");
            }

            existente.setCuentaPasivo(cuentaPasivo);
            existente.setCuentaLiquidacion(cuentaLiquidacion);

            CuentaTipoAporte actualizado = cuentaTipoAporteService.saveSingle(existente);
            return Response.status(Response.Status.OK)
                    .entity(actualizado).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al editar la cuenta por tipo de aporte: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT - Baja lógica: {@code estado = 0}. Idempotente — desactivar una fila ya inactiva no
     * es error.
     */
    @PUT
    @Path("/desactivar/{codigo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response desactivar(@PathParam("codigo") Long codigo) {
        System.out.println("LLEGA AL SERVICIO DESACTIVAR - CUENTA_TIPO_APORTE - codigo: " + codigo);
        try {
            CuentaTipoAporte existente = cuentaTipoAporteDaoService.find(new CuentaTipoAporte(), codigo);
            if (existente == null) {
                return errorSimple(Response.Status.NOT_FOUND, "No existe una configuracion con codigo " + codigo);
            }
            existente.setEstado(Long.valueOf(Estado.INACTIVO));
            CuentaTipoAporte actualizado = cuentaTipoAporteService.saveSingle(existente);
            return Response.status(Response.Status.OK)
                    .entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al desactivar la cuenta por tipo de aporte: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT - Reactiva una fila. Idempotente si ya estaba activa. Antes de reactivar valida que
     * NO exista otra fila activa para el mismo (tipo de aporte, empresa) — si no, la UK salta
     * como error de Oracle, o peor, si la UK no cubriera el estado, quedarían dos filas activas
     * y {@code selectByTipoAporteYEmpresa} devolvería la que salga primero: el asiento de
     * reclasificación cuadraría igual contra la cuenta equivocada, sin ningún error visible.
     */
    @PUT
    @Path("/activar/{codigo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activar(@PathParam("codigo") Long codigo) {
        System.out.println("LLEGA AL SERVICIO ACTIVAR - CUENTA_TIPO_APORTE - codigo: " + codigo);
        try {
            CuentaTipoAporte existente = cuentaTipoAporteDaoService.find(new CuentaTipoAporte(), codigo);
            if (existente == null) {
                return errorSimple(Response.Status.NOT_FOUND, "No existe una configuracion con codigo " + codigo);
            }

            if (!Long.valueOf(Estado.ACTIVO).equals(existente.getEstado())) {
                Long idTipoAporte = existente.getTipoAporte().getCodigo();
                Long idEmpresa = existente.getEmpresa().getCodigo();
                CuentaTipoAporte otraActiva = cuentaTipoAporteDaoService
                        .selectByTipoAporteYEmpresa(idTipoAporte, idEmpresa);
                if (otraActiva != null && !otraActiva.getCodigo().equals(codigo)) {
                    return errorSimple(Response.Status.CONFLICT,
                            "Ya existe una configuracion activa (codigo " + otraActiva.getCodigo()
                                    + ") para el tipo de aporte " + idTipoAporte
                                    + " y la empresa " + idEmpresa
                                    + "; desactivela antes de reactivar esta");
                }
                existente.setEstado(Long.valueOf(Estado.ACTIVO));
            }

            CuentaTipoAporte actualizado = cuentaTipoAporteService.saveSingle(existente);
            return Response.status(Response.Status.OK)
                    .entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al activar la cuenta por tipo de aporte: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Valida, para el POST, que las cuatro referencias obligatorias vengan con codigo y que
     * las cuatro existan de verdad — nunca dejar que una FK inválida llegue al INSERT como
     * violación de constraint.
     *
     * @return la Response de error a devolver, o {@code null} si todo es válido
     */
    private Response validarReferencias(CuentaTipoAporte registro) throws Throwable {
        if (registro.getTipoAporte() == null || registro.getTipoAporte().getCodigo() == null) {
            return errorSimple(Response.Status.BAD_REQUEST, "Debe indicar el tipo de aporte");
        }
        if (registro.getEmpresa() == null || registro.getEmpresa().getCodigo() == null) {
            return errorSimple(Response.Status.BAD_REQUEST, "Debe indicar la empresa");
        }
        if (registro.getCuentaPasivo() == null || registro.getCuentaPasivo().getCodigo() == null) {
            return errorSimple(Response.Status.BAD_REQUEST, "Debe indicar la cuenta de pasivo");
        }
        if (registro.getCuentaLiquidacion() == null || registro.getCuentaLiquidacion().getCodigo() == null) {
            return errorSimple(Response.Status.BAD_REQUEST, "Debe indicar la cuenta de liquidacion");
        }

        Long idTipoAporte = registro.getTipoAporte().getCodigo();
        if (tipoAporteDaoService.find(new TipoAporte(), idTipoAporte) == null) {
            return errorSimple(Response.Status.NOT_FOUND, "El tipo de aporte " + idTipoAporte + " no existe");
        }

        Long idEmpresa = registro.getEmpresa().getCodigo();
        if (empresaDaoService.find(new Empresa(), idEmpresa) == null) {
            return errorSimple(Response.Status.NOT_FOUND, "La empresa " + idEmpresa + " no existe");
        }

        Long idCuentaPasivo = registro.getCuentaPasivo().getCodigo();
        if (planCuentaDaoService.find(new PlanCuenta(), idCuentaPasivo) == null) {
            return errorSimple(Response.Status.NOT_FOUND, "La cuenta de pasivo " + idCuentaPasivo + " no existe");
        }

        Long idCuentaLiquidacion = registro.getCuentaLiquidacion().getCodigo();
        if (planCuentaDaoService.find(new PlanCuenta(), idCuentaLiquidacion) == null) {
            return errorSimple(Response.Status.NOT_FOUND,
                    "La cuenta de liquidacion " + idCuentaLiquidacion + " no existe");
        }

        return null;
    }

    private Response errorSimple(Response.Status status, String mensaje) {
        return Response.status(status).entity(mensaje).type(MediaType.APPLICATION_JSON).build();
    }
}
