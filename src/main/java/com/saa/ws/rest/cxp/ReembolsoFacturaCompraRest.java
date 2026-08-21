package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.ReembolsoFacturaCompraDaoService;
import com.saa.ejb.cxp.service.ReembolsoFacturaCompraService;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.ReembolsoFacturaCompra;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("rmbf")
public class ReembolsoFacturaCompraRest {

    @EJB private ReembolsoFacturaCompraDaoService reembolsoFacturaCompraDaoService;
    @EJB private ReembolsoFacturaCompraService    reembolsoFacturaCompraService;

    @Context private UriInfo context;

    public ReembolsoFacturaCompraRest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("getAll ReembolsoFacturaCompra");
        try {
            List<ReembolsoFacturaCompra> lista =
                    reembolsoFacturaCompraDaoService.selectAll(NombreEntidadesCompra.REEMBOLSO_FACTURA_COMPRA);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("getId ReembolsoFacturaCompra: " + id);
        try {
            ReembolsoFacturaCompra entidad =
                    reembolsoFacturaCompraDaoService.selectById(id,
                            NombreEntidadesCompra.REEMBOLSO_FACTURA_COMPRA);
            if (entidad == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("ReembolsoFacturaCompra ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getByFactura/{idFactura}") @Produces(MediaType.APPLICATION_JSON)
    public Response getByFactura(@PathParam("idFactura") Long idFactura) {
        System.out.println("getByFactura ReembolsoFacturaCompra: " + idFactura);
        try {
            return Response.ok(reembolsoFacturaCompraDaoService.selectByFactura(idFactura)).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error consultando reembolsos: " + e.getMessage()).build();
        }
    }

    @POST @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> datos) {
        System.out.println("selectByCriteria ReembolsoFacturaCompra");
        try {
            List<ReembolsoFacturaCompra> lista =
                    reembolsoFacturaCompraDaoService.selectByCriteria(datos,
                            NombreEntidadesCompra.REEMBOLSO_FACTURA_COMPRA);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(ReembolsoFacturaCompra registro) {
        System.out.println("post ReembolsoFacturaCompra");
        try {
            ReembolsoFacturaCompra resultado = reembolsoFacturaCompraService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(ReembolsoFacturaCompra registro) {
        System.out.println("put ReembolsoFacturaCompra id=" + registro.getId());
        try {
            ReembolsoFacturaCompra resultado = reembolsoFacturaCompraService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("delete ReembolsoFacturaCompra id=" + id);
        try {
            reembolsoFacturaCompraService.remove(java.util.Arrays.asList(id));
            return Response.status(Response.Status.OK)
                    .entity("ReembolsoFacturaCompra eliminado correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
