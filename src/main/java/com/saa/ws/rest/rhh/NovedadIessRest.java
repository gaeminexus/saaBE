package com.saa.ws.rest.rhh;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.NovedadIessDaoService;
import com.saa.ejb.rhh.service.ExportacionNovedadesIessService;
import com.saa.ejb.rhh.service.NovedadIessService;
import com.saa.model.rhh.ArchivoBatchIess;
import com.saa.model.rhh.NovedadIess;
import com.saa.model.rhh.NombreEntidadesRhh;

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

@Path("nvis")
public class NovedadIessRest {

    @EJB
    private NovedadIessDaoService novedadIessDaoService;

    @EJB
    private NovedadIessService novedadIessService;

    @EJB
    private ExportacionNovedadesIessService exportacionNovedadesIessService;

    @Context
    private UriInfo context;

    public NovedadIessRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - NOVEDADIESS");
        try {
            List<NovedadIess> lista = novedadIessDaoService.selectAll(NombreEntidadesRhh.NOVEDAD_IESS);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - NOVEDADIESS");
        try {
            NovedadIess registro = novedadIessDaoService.selectById(id, NombreEntidadesRhh.NOVEDAD_IESS);
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
    public Response put(NovedadIess registro) {
        System.out.println("LLEGA AL SERVICIO PUT - NOVEDADIESS");
        try {
            NovedadIess actualizado = novedadIessService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(NovedadIess registro) {
        System.out.println("LLEGA AL SERVICIO POST - NOVEDADIESS");
        try {
            NovedadIess creado = novedadIessService.saveSingle(registro);
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
        System.out.println("selectByCriteria de NOVEDADIESS");
        try {
            List<NovedadIess> lista = novedadIessService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - NOVEDADIESS");
        try {
            novedadIessService.remove(List.of(id));
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra a mano una novedad, calculandole el plazo legal.
     *
     * <p>Es la via del alta manual, y sustituye al POST del CRUD para esto: aquel graba lo
     * que le llega y deja <code>NVISFCLM</code> en nulo, y <b>una novedad sin plazo es
     * precisamente la que se escapa</b> — no sale vencida en ninguna pantalla ni tiene dias
     * restantes que mirar.</p>
     *
     * @param registro	: Novedad con empleado, tipo y fecha del hecho
     * @return			: La novedad creada, en PENDIENTE y con su fecha limite
     */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(NovedadIess registro) {
        System.out.println("LLEGA AL SERVICIO REGISTRAR - NOVEDADIESS");
        try {
            NovedadIess creado = novedadIessService.registrar(registro,
                    registro != null ? registro.getUsuarioRegistro() : null);
            return Response.status(Response.Status.CREATED).entity(creado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error al registrar la novedad: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Maquina de estados de la novedad
    //
    // Hasta ahora la pantalla la reproducia con el PUT del CRUD, y reproducir
    // no es impedir: cualquier cliente podia llevar una novedad aceptada de
    // vuelta a pendiente sin que nada se opusiera. Las transiciones validas
    // las decide el servicio; aqui solo se exponen.
    // =====================================================================

    /**
     * Marca la novedad como enviada al IESS.
     *
     * @param id		: Id de la novedad
     * @param datos		: Puede traer "lote" y "usuario"
     * @return			: La novedad actualizada
     */
    @PUT
    @Path("/marcarEnviada/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response marcarEnviada(@PathParam("id") Long id, Map<String, String> datos) {
        System.out.println("LLEGA AL SERVICIO MARCAR ENVIADA - NOVEDADIESS");
        try {
            NovedadIess actualizado = novedadIessService.marcarEnviada(id, valor(datos, "lote"),
                    valor(datos, "usuario"));
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error al marcar como enviada: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Marca la novedad como aceptada por el IESS.
     *
     * @param id		: Id de la novedad
     * @param datos		: Puede traer "usuario"
     * @return			: La novedad actualizada
     */
    @PUT
    @Path("/marcarAceptada/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response marcarAceptada(@PathParam("id") Long id, Map<String, String> datos) {
        System.out.println("LLEGA AL SERVICIO MARCAR ACEPTADA - NOVEDADIESS");
        try {
            NovedadIess actualizado = novedadIessService.marcarAceptada(id, valor(datos, "usuario"));
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error al marcar como aceptada: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Marca la novedad como rechazada por el IESS. El motivo es obligatorio.
     *
     * @param id		: Id de la novedad
     * @param datos		: Debe traer "motivo"; puede traer "usuario"
     * @return			: La novedad actualizada
     */
    @PUT
    @Path("/marcarRechazada/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response marcarRechazada(@PathParam("id") Long id, Map<String, String> datos) {
        System.out.println("LLEGA AL SERVICIO MARCAR RECHAZADA - NOVEDADIESS");
        try {
            NovedadIess actualizado = novedadIessService.marcarRechazada(id, valor(datos, "motivo"),
                    valor(datos, "usuario"));
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error al marcar como rechazada: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula la novedad: no habia que reportarla. No la borra.
     *
     * @param id		: Id de la novedad
     * @param datos		: Puede traer "motivo" y "usuario"
     * @return			: La novedad actualizada
     */
    @PUT
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, String> datos) {
        System.out.println("LLEGA AL SERVICIO ANULAR - NOVEDADIESS");
        try {
            NovedadIess actualizado = novedadIessService.anular(id, valor(datos, "motivo"),
                    valor(datos, "usuario"));
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error al anular: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Genera el archivo de carga batch de un tipo de novedad para el periodo.
     *
     * <p>Devuelve texto plano para que el cliente lo baje como archivo. Cuando falta un
     * dato o un codigo del catalogo sigue en '?', <b>responde 400 con el motivo en el
     * cuerpo</b> en vez de un archivo a medias: subir un archivo con un '?' dentro hace
     * que el IESS lo rechace entero, y el rechazo llega dias despues.</p>
     *
     * <p>Recibe <code>idPeriodo</code> y no un par de fechas <b>para que la ventana de
     * novedades se calcule en un solo sitio</b>. Si la mandara el cliente, cliente y
     * servidor podrian estar mirando conjuntos distintos sin que nada lo delatara.</p>
     *
     * @param datos	: Cuerpo con "idPeriodo", "tipoNovedad" y opcionalmente "usuario"
     * @return		: El archivo, o 400 con el motivo
     */
    @POST
    @Path("/exportarBatch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportarBatch(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO EXPORTAR BATCH - NOVEDADIESS");
        try {
            Long idPeriodo = numero(datos, "idPeriodo");
            Long tipo = numero(datos, "tipoNovedad");
            String usuario = datos != null && datos.get("usuario") != null
                    ? String.valueOf(datos.get("usuario")) : null;
            ArchivoBatchIess archivo = exportacionNovedadesIessService.generarArchivo(idPeriodo, tipo, usuario);
            // DEVUELVE JSON, NO UN BLOB, Y EL AVISO VA DENTRO DEL CUERPO.
            //
            // La version anterior mandaba el archivo como texto y el aviso en cabeceras
            // X-Saa-* mas el nombre en Content-Disposition. Cruzando origen se habrian
            // perdido las dos cosas a la vez y en silencio: ninguna de esas cabeceras es
            // safelisted y el filtro CORS de Undertow no declara Access-Control-Expose-Headers.
            // Exponerlas obligaria a tocar el .cli de cada instalacion y a que nadie lo
            // olvidara nunca; una proteccion que depende de un script externo no es una
            // proteccion. El archivo son veinte lineas de texto: cabe en el JSON, y el
            // frontend arma la descarga con el nombre que viene dentro.
            return Response.status(Response.Status.OK).entity(archivo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error al exportar el archivo batch: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Lee una clave del cuerpo tolerando que el cuerpo no venga.
     *
     * @param datos	: Cuerpo de la peticion
     * @param clave	: Clave buscada
     * @return		: El valor, o null
     */
    private String valor(Map<String, String> datos, String clave) {
        return datos == null ? null : datos.get(clave);
    }

    /**
     * Lee un numero del cuerpo. Jackson puede entregarlo como Integer o como Long segun su
     * magnitud, asi que se normaliza en vez de castear.
     *
     * @param datos	: Cuerpo de la peticion
     * @param clave	: Clave buscada
     * @return		: El valor como Long, o null
     */
    private Long numero(Map<String, Object> datos, String clave) {
        Object valor = datos == null ? null : datos.get(clave);
        if (valor == null) {
            return null;
        }
        return valor instanceof Number ? Long.valueOf(((Number) valor).longValue())
                : Long.valueOf(String.valueOf(valor).trim());
    }
}
