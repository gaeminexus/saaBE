package com.saa.ws.rest.rhh;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.AnticipoEmpleadoDaoService;
import com.saa.ejb.rhh.service.AnticipoEmpleadoService;
import com.saa.model.rhh.AnticipoEmpleado;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST para Anticipos a Empleados (RHH).
 * Base path: /ante
 *
 * Endpoints principales:
 *   POST /ante/solicitar               → solicita un anticipo (estado SOLICITADO)
 *   POST /ante/aprobar/{id}             → aprueba y registra el pago (PagoProgramado)
 *   POST /ante/anular/{id}              → anula (sólo SOLICITADO o APROBADO sin pago confirmado)
 *   GET  /ante/listar                   → listado con filtros
 *   GET  /ante/vigente/{idEmpleado}     → anticipo vivo del empleado, si tiene uno
 */
@Path("ante")
public class AnticipoEmpleadoRest {

	@EJB
	private AnticipoEmpleadoDaoService anticipoEmpleadoDaoService;

	@EJB
	private AnticipoEmpleadoService anticipoEmpleadoService;

	public AnticipoEmpleadoRest() {}

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll() {
		try {
			List<AnticipoEmpleado> lista = anticipoEmpleadoDaoService.selectAll(NombreEntidadesRhh.ANTICIPO_EMPLEADO);
			return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener AnticipoEmpleado: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@GET
	@Path("/getId/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getId(@PathParam("id") Long id) {
		try {
			AnticipoEmpleado entidad = anticipoEmpleadoDaoService.selectById(id, NombreEntidadesRhh.ANTICIPO_EMPLEADO);
			if (entidad == null) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("AnticipoEmpleado con ID " + id + " no encontrado")
						.type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener AnticipoEmpleado: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(AnticipoEmpleado registro) {
		try {
			AnticipoEmpleado resultado = anticipoEmpleadoService.saveSingle(registro);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar AnticipoEmpleado: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(AnticipoEmpleado registro) {
		try {
			AnticipoEmpleado resultado = anticipoEmpleadoService.saveSingle(registro);
			return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al crear AnticipoEmpleado: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") Long id) {
		try {
			List<Long> ids = new java.util.ArrayList<>();
			ids.add(id);
			anticipoEmpleadoService.remove(ids);
			return Response.status(Response.Status.OK)
					.entity("AnticipoEmpleado eliminado correctamente")
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al eliminar AnticipoEmpleado: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	@POST
	@Path("selectByCriteria")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response selectByCriteria(List<DatosBusqueda> datos) {
		try {
			List<AnticipoEmpleado> result = anticipoEmpleadoService.selectByCriteria(datos);
			return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error en selectByCriteria AnticipoEmpleado: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Solicita un anticipo.
	 * Body esperado:
	 * {
	 *   "idEmpleado": 45, "valor": 300.00, "numeroCuotas": 3,
	 *   "fechaInicioDescuento": "2026-09-01", "motivo": "Gastos médicos",
	 *   "observacion": "", "idUsuario": 5
	 * }
	 */
	@POST
	@Path("/solicitar")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response solicitar(Map<String, Object> datos) {
		System.out.println("LLEGA AL SERVICIO POST /ante/solicitar");
		try {
			Long idEmpleado = toLong(datos.get("idEmpleado"));
			Double valor = toDouble(datos.get("valor"));
			Integer numeroCuotas = toInteger(datos.get("numeroCuotas"));
			String fechaInicioDescuento = (String) datos.get("fechaInicioDescuento");
			String motivo = (String) datos.get("motivo");
			String observacion = (String) datos.get("observacion");
			Long idUsuario = toLong(datos.get("idUsuario"));

			if (idEmpleado == null || valor == null || numeroCuotas == null) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity("Debe enviar idEmpleado, valor y numeroCuotas.")
						.type(MediaType.APPLICATION_JSON).build();
			}

			java.time.LocalDate fecha = (fechaInicioDescuento != null && !fechaInicioDescuento.trim().isEmpty())
					? java.time.LocalDate.parse(fechaInicioDescuento.trim()) : null;

			AnticipoEmpleado resultado = anticipoEmpleadoService.solicitar(
					idEmpleado, valor, numeroCuotas, fecha, motivo, observacion, idUsuario);
			return Response.status(Response.Status.CREATED).entity(resultado)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			System.err.println("ERROR en solicitar anticipo: " + e.getMessage());
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Error al solicitar el anticipo: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Aprueba un anticipo y registra su pago.
	 * Body esperado:
	 * {
	 *   "idCuentaBancariaOrigen": 4, "formaPago": 3, "debitoAutomatico": false,
	 *   "referencia": "CHQ-001234", "idUsuario": 5
	 * }
	 * idCuentaBancariaOrigen y formaPago son OPCIONALES (decisión 2026-08-30): si se
	 * omiten, el pago nace POR_APROBAR y tesorería asigna la cuenta y la forma de pago
	 * después con POST /pgtr/aprobar (bandeja de aprobación de pagos). Si se envía
	 * idCuentaBancariaOrigen, formaPago sólo admite 3 (Cheque) o 4 (Débito automático)
	 * — no hay datos bancarios del empleado capturados para transferencia.
	 */
	@POST
	@Path("/aprobar/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response aprobar(@PathParam("id") Long id, Map<String, Object> datos) {
		System.out.println("LLEGA AL SERVICIO POST /ante/aprobar/" + id);
		try {
			Long idCuentaBancariaOrigen = toLong(datos.get("idCuentaBancariaOrigen"));
			Long formaPago = toLong(datos.get("formaPago"));
			Boolean debitoAutomatico = (datos.get("debitoAutomatico") instanceof Boolean)
					? (Boolean) datos.get("debitoAutomatico") : Boolean.FALSE;
			String referencia = (String) datos.get("referencia");
			Long idUsuario = toLong(datos.get("idUsuario"));

			Map<String, Object> resultado = anticipoEmpleadoService.aprobar(
					id, idCuentaBancariaOrigen, formaPago, Boolean.TRUE.equals(debitoAutomatico),
					referencia, idUsuario);
			return Response.status(Response.Status.OK).entity(resultado)
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			System.err.println("ERROR en aprobar anticipo: " + e.getMessage());
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Error al aprobar el anticipo: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Anula un anticipo.
	 * Body esperado: { "motivo": "...", "idUsuario": 5 }
	 */
	@POST
	@Path("/anular/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
		System.out.println("LLEGA AL SERVICIO POST /ante/anular/" + id);
		try {
			String motivo = (datos != null) ? (String) datos.get("motivo") : null;
			Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

			anticipoEmpleadoService.anular(id, motivo, idUsuario);
			return Response.status(Response.Status.OK)
					.entity("Anticipo anulado correctamente")
					.type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			System.err.println("ERROR en anular anticipo: " + e.getMessage());
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Error al anular el anticipo: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Listado de anticipos con filtros opcionales.
	 */
	@GET
	@Path("/listar")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listar(
			@QueryParam("idEmpresa") Long idEmpresa,
			@QueryParam("idEmpleado") Long idEmpleado,
			@QueryParam("estado") Long estado) {
		try {
			List<AnticipoEmpleado> resultado = anticipoEmpleadoService.listar(idEmpresa, idEmpleado, estado);
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al listar anticipos: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	/**
	 * Anticipo vivo de un empleado (SOLICITADO, APROBADO, PAGADO o
	 * EN_DESCUENTO), si tiene uno. 404 si no tiene ninguno.
	 */
	@GET
	@Path("/vigente/{idEmpleado}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response vigente(@PathParam("idEmpleado") Long idEmpleado) {
		try {
			AnticipoEmpleado resultado = anticipoEmpleadoService.consultarPorEmpleado(idEmpleado);
			if (resultado == null) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("El empleado " + idEmpleado + " no tiene ningún anticipo vigente.")
						.type(MediaType.APPLICATION_JSON).build();
			}
			return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al consultar el anticipo vigente: " + e.getMessage())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}

	// ── Helpers de conversión del JSON ───────────────────────────────────────

	private Long toLong(Object valor) {
		if (valor == null) return null;
		if (valor instanceof Number) return ((Number) valor).longValue();
		try {
			return Long.valueOf(valor.toString().trim());
		} catch (Exception e) {
			return null;
		}
	}

	private Integer toInteger(Object valor) {
		if (valor == null) return null;
		if (valor instanceof Number) return ((Number) valor).intValue();
		try {
			return Integer.valueOf(valor.toString().trim());
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
