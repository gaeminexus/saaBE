package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.basico.util.UsuarioInvalidoException;
import com.saa.ejb.crd.dao.DestinoSobranteDaoService;
import com.saa.ejb.crd.service.DestinoSobranteService;
import com.saa.model.crd.DestinoSobrante;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Destino del sobrante por partícipe en una carga Petro/ARCH (CRD.DSSB, B8). Toda escritura exige {@code ?usuario=}
 * (se valida primero; sin usuario válido responde 400 y no graba). No hay DELETE: se anula.
 */
@Path("dssb")
public class DestinoSobranteRest {

    @EJB
    private DestinoSobranteDaoService destinoSobranteDaoService;

    @EJB
    private DestinoSobranteService destinoSobranteService;

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("getAll de DestinoSobrante");
        try {
            return Response.ok(destinoSobranteDaoService.selectAll(NombreEntidadesCredito.DESTINO_SOBRANTE))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener destinos del sobrante: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("getId de DestinoSobrante: " + id);
        try {
            DestinoSobrante destino = destinoSobranteService.selectById(id);
            return Response.ok(destino).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el destino del sobrante: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** Todos los destinos de una carga, activos y anulados. */
    @GET
    @Path("/selectByCarga/{idCarga}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCarga(@PathParam("idCarga") Long idCarga) {
        System.out.println("selectByCarga de DestinoSobrante: " + idCarga);
        try {
            return Response.ok(destinoSobranteService.selectByCarga(idCarga))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los destinos de la carga: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de DestinoSobrante");
        try {
            return Response.ok(destinoSobranteService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** Registra (o reemplaza) el destino del sobrante de un partícipe. Body: DestinoSobrante; {@code ?usuario=} obligatorio. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DestinoSobrante registro, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO POST DE DestinoSobrante - usuario: " + usuario);
        return registrar(registro, usuario, Response.Status.CREATED);
    }

    /** Igual que el POST: el destino activo de la pareja (carga, partícipe) se reemplaza. */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DestinoSobrante registro, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO PUT DE DestinoSobrante - usuario: " + usuario);
        return registrar(registro, usuario, Response.Status.OK);
    }

    @POST
    @Path("/anular/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, @QueryParam("usuario") String usuario) {
        System.out.println("anular de DestinoSobrante: " + id + " - usuario: " + usuario);
        try {
            return Response.ok(destinoSobranteService.anular(id, usuario)).type(MediaType.APPLICATION_JSON).build();
        } catch (UsuarioInvalidoException e) {
            return respuestaUsuarioInvalido(e);
        } catch (IncomeException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular el destino del sobrante: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Response registrar(DestinoSobrante registro, String usuario, Response.Status estadoOk) {
        try {
            return Response.status(estadoOk).entity(destinoSobranteService.registrar(registro, usuario))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (UsuarioInvalidoException e) {
            return respuestaUsuarioInvalido(e);
        } catch (IncomeException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar el destino del sobrante: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Response respuestaUsuarioInvalido(UsuarioInvalidoException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("No se puede registrar el destino sin un usuario válido: " + e.getMotivo())
                .type(MediaType.APPLICATION_JSON).build();
    }
}
