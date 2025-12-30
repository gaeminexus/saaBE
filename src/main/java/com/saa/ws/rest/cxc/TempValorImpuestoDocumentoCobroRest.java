package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.TempValorImpuestoDocumentoCobroDaoService;
import com.saa.ejb.cxc.service.TempValorImpuestoDocumentoCobroService;
import com.saa.model.cxc.TempValorImpuestoDocumentoCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

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

@Path("tidc")
public class TempValorImpuestoDocumentoCobroRest {

    @EJB
    private TempValorImpuestoDocumentoCobroDaoService tempValorImpuestoDocumentoCobroDaoService;

    @EJB
    private TempValorImpuestoDocumentoCobroService tempValorImpuestoDocumentoCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TempValorImpuestoDocumentoCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of TempValorImpuestoDocumentoCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TempValorImpuestoDocumentoCobro> lista = tempValorImpuestoDocumentoCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_VALOR_IMPUESTO_DOCUMENTO_COBRO);
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
            TempValorImpuestoDocumentoCobro registro = tempValorImpuestoDocumentoCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_VALOR_IMPUESTO_DOCUMENTO_COBRO);
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
    public Response put(TempValorImpuestoDocumentoCobro registro) {
        System.out.println("LLEGA AL SERVICIO PUT");
        try {
            TempValorImpuestoDocumentoCobro actualizado = tempValorImpuestoDocumentoCobroService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TempValorImpuestoDocumentoCobro registro) {
        System.out.println("LLEGA AL POST");
        try {
            TempValorImpuestoDocumentoCobro creado = tempValorImpuestoDocumentoCobroService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(creado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TempValorImpuestoDocumentoCobro");
        try {
            List<TempValorImpuestoDocumentoCobro> lista = tempValorImpuestoDocumentoCobroService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en búsqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE");
        try {
            TempValorImpuestoDocumentoCobro elimina = new TempValorImpuestoDocumentoCobro();
            tempValorImpuestoDocumentoCobroDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}