package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.TitularDaoService;
import com.saa.ejb.tsr.service.TitularService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Titular;

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

@Path("ttlr")
public class TitularRest {

    @EJB
    private TitularDaoService titularDaoService;
    
    @EJB
    private TitularService titularService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TitularRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Persona.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Titular> lista = titularDaoService.selectAll(NombreEntidadesTesoreria.TITULAR);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener personas: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de Persona por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Titular persona = titularDaoService.selectById(id, NombreEntidadesTesoreria.TITULAR);
            if (persona == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Persona con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(persona).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Titular registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TITULAR");
        try {
            Titular resultado = titularService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            if (esViolacionUnicidadIdentificacion(e)) {
                return respuestaTitularDuplicado(registro, e);
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Titular registro) {
        System.out.println("LLEGA AL SERVICIO POST - PERSONA");
        try {
        	Titular resultado = titularService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            if (esViolacionUnicidadIdentificacion(e)) {
                return respuestaTitularDuplicado(registro, e);
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear persona: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Detecta si una excepción (o alguna en su cadena de causas) señala la
     * violación del índice único UK_TTLR_IDNT_ESTD (TTLRIDNT, TTLRESTD).
     * <p>
     * Cubre dos casos: el {@code IncomeException} con mensaje amigable que ya
     * lanza {@code TitularServiceImpl.saveSingle} al validar antes de grabar,
     * y —como red de seguridad— el {@code ORA-00001}/nombre del índice crudo
     * de una {@code PersistenceException}, por si alguien graba un titular
     * por una vía que se saltó esa validación.
     */
    private boolean esViolacionUnicidadIdentificacion(Throwable e) {
        Throwable actual = e;
        int vueltas = 0;
        while (actual != null && vueltas < 10) {
            String msg = actual.getMessage();
            if (msg != null && (msg.contains("Ya existe un titular activo con la identificación")
                    || msg.contains("ORA-00001") || msg.contains("UK_TTLR_IDNT_ESTD"))) {
                return true;
            }
            actual = actual.getCause();
            vueltas++;
        }
        return false;
    }

    /**
     * Construye la respuesta 409 para un titular duplicado: mensaje amigable
     * (el mismo texto tanto si lo detectó la validación previa como la red de
     * seguridad) + el titular existente (código, identificación, nombre) para
     * que el frontend pueda ofrecer "usar el existente" en vez de crear otro
     * — en este modelo no hay tabla de proveedores separada: lo normal es
     * reutilizar el titular y agregarle el rol de cliente/proveedor que falte.
     */
    private Response respuestaTitularDuplicado(Titular intentado, Throwable causaOriginal) {
        String identificacion = intentado != null ? intentado.getIdentificacion() : null;
        Long estado = (intentado != null && intentado.getEstado() != null)
                ? intentado.getEstado() : Long.valueOf(com.saa.rubros.Estado.ACTIVO);
        java.util.Map<String, Object> respuesta = new java.util.HashMap<>();
        try {
            Titular existente = titularDaoService.selectByIdentificacion(identificacion, estado);
            if (existente != null) {
                String nombreExistente = (existente.getRazonSocial() != null && !existente.getRazonSocial().trim().isEmpty())
                        ? existente.getRazonSocial() : existente.getNombre();
                respuesta.put("mensaje", "Ya existe un titular activo con la identificación "
                        + identificacion + ": " + nombreExistente + " (código " + existente.getCodigo() + ")");
                java.util.Map<String, Object> titularExistente = new java.util.HashMap<>();
                titularExistente.put("codigo", existente.getCodigo());
                titularExistente.put("identificacion", existente.getIdentificacion());
                titularExistente.put("nombre", nombreExistente);
                respuesta.put("titularExistente", titularExistente);
            } else {
                // No se pudo re-consultar (p.ej. otro estado); se devuelve el
                // mensaje original tal cual, sin el detalle del existente.
                respuesta.put("mensaje", causaOriginal.getMessage());
            }
        } catch (Throwable lookupEx) {
            respuesta.put("mensaje", causaOriginal.getMessage());
        }
        return Response.status(Response.Status.CONFLICT).entity(respuesta).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * POST method for updating or creating an instance of PersonaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de PERSONA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(titularService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de Persona por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PERSONA");
        try {
            Titular elimina = new Titular();
            titularDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar titular: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
