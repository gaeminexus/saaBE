package com.saa.ws.rest.crd;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.ContratoDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.ParticipeDaoService;
import com.saa.ejb.crd.service.ContratoService;
import com.saa.ejb.crd.service.VigenciaContratoService;
import com.saa.ejb.crd.service.dto.ContratoConVigenciasDTO;
import com.saa.model.crd.Contrato;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Participe;
import com.saa.rubros.EstadoContrato;

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

@Path("cntr")
public class ContratoRest {

    @EJB
    private ContratoDaoService contratoDaoService;

    @EJB
    private ContratoService contratoService;

    @EJB
    private VigenciaContratoService vigenciaContratoService;

    @EJB
    private ParticipeDaoService participeDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public ContratoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of ContratoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Contrato> lista = contratoDaoService.selectAll(NombreEntidadesCredito.CONTRATO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener contratos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Contrato contrato = contratoDaoService.selectById(id, NombreEntidadesCredito.CONTRATO);
            if (contrato == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Contrato con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(contrato).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener contrato: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Contrato registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CONTRATO");
        try {
            Contrato resultado = contratoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar contrato: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Contrato registro) {
        System.out.println("LLEGA AL SERVICIO POST - CONTRATO");
        try {
            Contrato resultado = contratoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear contrato: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of ContratoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Contrato");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(contratoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * GET /rest/cntr/porEntidad/{idEntidad} — contrato ACTIVO más reciente de la entidad,
     * con el espejo de la vigencia abierta y su historial completo de vigencias. Contrato
     * de API congelado en docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §4.1,
     * actualizado el 2026-08-27: una entidad SIN contrato activo ya no es 404 — es un estado
     * válido (aún no se le creó un contrato) y se devuelve 200 con la cabecera poblada y el
     * contrato en blanco. El 404 queda solo para cuando la ENTIDAD no existe.
     */
    @GET
    @Path("/porEntidad/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO GET porEntidad - CONTRATO - idEntidad: " + idEntidad);
        try {
            Entidad entidad = entidadDaoService.findById(idEntidad);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("La entidad " + idEntidad + " no existe")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            ContratoConVigenciasDTO dto = new ContratoConVigenciasDTO();
            dto.setIdEntidad(entidad.getCodigo());
            dto.setIdentificacion(entidad.getNumeroIdentificacion());
            dto.setRazonSocial(entidad.getRazonSocial());
            dto.setVigencias(new ArrayList<>());

            List<Participe> participes = participeDaoService.selectByEntidad(idEntidad);
            dto.setRemuneracionUnificada(!participes.isEmpty() ? participes.get(0).getRemuneracionUnificada() : null);

            Contrato contrato = contratoDaoService.selectActivoPorEntidad(idEntidad);
            if (contrato != null) {
                dto.setIdContrato(contrato.getCodigo());
                dto.setEstado(contrato.getEstado());
                dto.setEstadoTexto(textoEstadoContrato(contrato.getEstado()));
                dto.setMontoJubilacion(contrato.getMontoAporteJubilacion());
                dto.setMontoCesantia(contrato.getMontoAporteCesantia());
                dto.setPorcentajeJubilacion(contrato.getPorcentajeAporteJubilacion());
                dto.setPorcentajeCesantia(contrato.getPorcentajeAporteIndividual());
                dto.setVigencias(vigenciaContratoService.selectByContrato(contrato.getCodigo()));
            }
            // Sin contrato activo: idContrato/estado/estadoTexto/montos/porcentajes quedan
            // null y vigencias queda vacía — no es un error, ver el javadoc de arriba.

            return Response.status(Response.Status.OK).entity(dto).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el contrato de la entidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** Mismo criterio que modoTexto/tipoMovimientoTexto: el backend resuelve el catálogo, no el cliente. */
    private String textoEstadoContrato(Long estado) {
        if (estado == null) {
            return null;
        }
        if (estado == EstadoContrato.ACTIVO) {
            return "ACTIVO";
        }
        if (estado == EstadoContrato.INACTIVO) {
            return "INACTIVO";
        }
        return String.valueOf(estado);
    }

    /**
     * DELETE method for deleting an instance of ContratoRest
     * 
     * @param id identifier for the resource
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - CONTRATO");
        try {
            Contrato elimina = new Contrato();
            contratoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar contrato: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
