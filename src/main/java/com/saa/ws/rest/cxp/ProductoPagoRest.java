package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.ProductoPagoDaoService;
import com.saa.ejb.cxp.service.ProductoPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ProductoPago;

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

@Path("prdp")
public class ProductoPagoRest {

    @EJB
    private ProductoPagoDaoService ProductoPagoDaoService;

    @EJB
    private ProductoPagoService ProductoPagoService;

    @Context
    private UriInfo context;

    public ProductoPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ProductoPago> getAll() throws Throwable {
        return ProductoPagoDaoService.selectAll(NombreEntidadesPago.PRODUCTO_PAGO);
    }

    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ProductoPago getId(@PathParam("id") Long id) throws Throwable {
        return ProductoPagoDaoService.selectById(id, NombreEntidadesPago.PRODUCTO_PAGO);
    }
    

    @PUT
    @Consumes("application/json")
    public ProductoPago put(ProductoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - PRODUCTO_PAGO");
        return ProductoPagoService.saveSingle(registro);
    }

    @POST
    @Consumes("application/json")
    public ProductoPago post(ProductoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - PRODUCTO_PAGO");
        return ProductoPagoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PRODUCTO_PAGO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(ProductoPagoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - PRODUCTO_PAGO");
        ProductoPago elimina = new ProductoPago();
        ProductoPagoDaoService.remove(elimina, id);
    }
}
