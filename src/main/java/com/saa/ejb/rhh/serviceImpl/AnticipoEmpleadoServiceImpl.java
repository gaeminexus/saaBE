package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.rhh.dao.AnticipoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.EmpleadoDaoService;
import com.saa.ejb.rhh.service.AnticipoEmpleadoService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.AnticipoEmpleado;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.scp.Usuario;
import com.saa.rubros.EstadoAnticipoEmpleado;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.FormaPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.RhhEstadoEmpleado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * Implementación de AnticipoEmpleadoService.
 */
@Stateless
public class AnticipoEmpleadoServiceImpl implements AnticipoEmpleadoService {

	@EJB
	private AnticipoEmpleadoDaoService anticipoEmpleadoDaoService;

	@EJB
	private EmpleadoDaoService empleadoDaoService;

	@EJB
	private PagoProgramadoService pagoProgramadoService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// EntityService
	// =====================================================================

	@Override
	public AnticipoEmpleado selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById AnticipoEmpleado con id: " + id);
		return anticipoEmpleadoDaoService.selectById(id, NombreEntidadesRhh.ANTICIPO_EMPLEADO);
	}

	@Override
	public List<AnticipoEmpleado> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll AnticipoEmpleadoService");
		List<AnticipoEmpleado> result = anticipoEmpleadoDaoService.selectAll(NombreEntidadesRhh.ANTICIPO_EMPLEADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total AnticipoEmpleado no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public List<AnticipoEmpleado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria AnticipoEmpleadoService");
		List<AnticipoEmpleado> result =
				anticipoEmpleadoDaoService.selectByCriteria(datos, NombreEntidadesRhh.ANTICIPO_EMPLEADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio AnticipoEmpleado no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public AnticipoEmpleado saveSingle(AnticipoEmpleado entidad) throws Throwable {
		System.out.println("saveSingle - AnticipoEmpleado");
		return anticipoEmpleadoDaoService.save(entidad, entidad.getCodigo());
	}

	@Override
	public void save(List<AnticipoEmpleado> lista) throws Throwable {
		for (AnticipoEmpleado registro : lista) {
			saveSingle(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		AnticipoEmpleado entidad = new AnticipoEmpleado();
		for (Long registro : id) {
			anticipoEmpleadoDaoService.remove(entidad, registro);
		}
	}

	// =====================================================================
	// Ciclo del anticipo
	// =====================================================================

	@Override
	public AnticipoEmpleado solicitar(Long idEmpleado, Double valor, Integer numeroCuotas,
			LocalDate fechaInicioDescuento, String motivo, String observacion, Long idUsuario)
			throws Throwable {
		System.out.println("=== solicitar anticipo | idEmpleado=" + idEmpleado + " | valor=" + valor
				+ " | numeroCuotas=" + numeroCuotas + " ===");

		Empleado empleado = empleadoDaoService.selectById(idEmpleado, NombreEntidadesRhh.EMPLEADO);
		if (empleado == null) {
			throw new IncomeException("No se encontró el empleado con ID: " + idEmpleado);
		}
		if (!Long.valueOf(RhhEstadoEmpleado.ACTIVO).equals(empleado.getEstado())) {
			throw new IncomeException("El empleado " + nombreCompleto(empleado) + " no está activo.");
		}
		if (valor == null || valor <= 0) {
			throw new IncomeException("El valor del anticipo debe ser mayor a cero.");
		}
		if (numeroCuotas == null || numeroCuotas < 1) {
			throw new IncomeException("El número de cuotas debe ser al menos 1.");
		}

		AnticipoEmpleado vigente = anticipoEmpleadoDaoService.selectVigenteByEmpleado(idEmpleado);
		if (vigente != null) {
			throw new IncomeException("El empleado " + nombreCompleto(empleado)
					+ " ya tiene un anticipo abierto (código " + vigente.getCodigo()
					+ ", estado " + vigente.getEstado() + "). Sólo puede tener uno a la vez.");
		}

		AnticipoEmpleado anticipo = new AnticipoEmpleado();
		anticipo.setEmpleado(empleado);
		anticipo.setFecha(LocalDate.now());
		anticipo.setValor(valor);
		anticipo.setNumeroCuotas(Long.valueOf(numeroCuotas.longValue()));
		// Cuota "de referencia" redondeada; la última cuota real (generada al
		// confirmarse el pago, ver PagoProgramadoServiceImpl.generaCuotasAnticipo)
		// absorbe el residuo de redondeo para que la suma cuadre exacto con valor.
		anticipo.setValorCuota(RedondeoNomina.divide(valor, Double.valueOf(numeroCuotas.doubleValue())));
		anticipo.setSaldo(valor);
		anticipo.setFechaInicioDescuento(fechaInicioDescuento);
		anticipo.setMotivo(motivo);
		anticipo.setObservacion(observacion);
		anticipo.setEstado(Long.valueOf(EstadoAnticipoEmpleado.SOLICITADO));
		anticipo.setFechaRegistro(LocalDateTime.now());
		anticipo.setUsuarioRegistro(nombreUsuario(idUsuario));

		anticipo = anticipoEmpleadoDaoService.save(anticipo, null);
		System.out.println("✓ Anticipo solicitado: id=" + anticipo.getCodigo()
				+ " | empleado=" + nombreCompleto(empleado));
		return anticipo;
	}

	@Override
	public Map<String, Object> aprobar(Long idAnticipo, Long idCuentaBancariaOrigen, Long formaPago,
			boolean debitoAutomatico, String referencia, Long idUsuario) throws Throwable {
		System.out.println("=== aprobar anticipo | idAnticipo=" + idAnticipo
				+ " | idCuentaBancariaOrigen=" + idCuentaBancariaOrigen + " | formaPago=" + formaPago + " ===");

		AnticipoEmpleado anticipo = anticipoEmpleadoDaoService.selectById(idAnticipo, NombreEntidadesRhh.ANTICIPO_EMPLEADO);
		if (anticipo == null) {
			throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
		}
		if (!Long.valueOf(EstadoAnticipoEmpleado.SOLICITADO).equals(anticipo.getEstado())) {
			throw new IncomeException("Sólo se puede aprobar un anticipo SOLICITADO. Estado actual: "
					+ anticipo.getEstado());
		}

		// El beneficiario se arma desde Empleado (identificación + nombres): no
		// hay datos bancarios del empleado capturados en este circuito (esa es
		// la decisión tomada — ver ANTICIPOS-TRABAJADORES.md), así que no hay
		// forma de armar una transferencia. Cheque o débito automático, igual
		// que caja chica.
		long fp = (formaPago != null) ? formaPago.longValue()
				: (debitoAutomatico ? FormaPagoProgramado.DEBITO_AUTOMATICO : FormaPagoProgramado.TRANSFERENCIA);
		if (fp != FormaPagoProgramado.CHEQUE && fp != FormaPagoProgramado.DEBITO_AUTOMATICO) {
			throw new IncomeException("El anticipo a empleado debe pagarse con cheque o débito automático:"
					+ " no hay datos bancarios del empleado capturados para generar una transferencia.");
		}

		Empleado empleado = anticipo.getEmpleado();
		Long idEmpresa = (empleado.getEmpresa() != null) ? empleado.getEmpresa().getCodigo() : null;
		if (idEmpresa == null) {
			throw new IncomeException("El empleado " + nombreCompleto(empleado)
					+ " no tiene empresa asignada: sin ella no se puede registrar el pago.");
		}

		anticipo.setUsuarioAprueba(idUsuario);
		anticipo.setFechaAprobacion(LocalDate.now());
		anticipo.setEstado(Long.valueOf(EstadoAnticipoEmpleado.APROBADO));
		anticipo = anticipoEmpleadoDaoService.save(anticipo, anticipo.getCodigo());
		em.flush();

		BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
		beneficiario.setNombre(nvl(empleado.getNombres(), nombreCompleto(empleado)));
		beneficiario.setIdentificacion(empleado.getIdentificacion());

		Map<String, Object> resultadoPago = pagoProgramadoService.registrarPagoDeOrigenExterno(
				OrigenPagoExterno.RHH_ANTICIPO_EMPLEADO, anticipo.getCodigo(), idEmpresa,
				idCuentaBancariaOrigen, anticipo.getValor(), anticipo.getFecha().toString(),
				beneficiario, null, "Anticipo a colaborador " + nombreCompleto(empleado),
				idUsuario, (fp == FormaPagoProgramado.DEBITO_AUTOMATICO), referencia, Long.valueOf(fp));

		Long idPago = (Long) resultadoPago.get("pago");
		if (idPago != null) {
			com.saa.model.cxp.PagoProgramado pago = em.find(com.saa.model.cxp.PagoProgramado.class, idPago);
			anticipo.setPagoProgramado(pago);
			anticipo = anticipoEmpleadoDaoService.save(anticipo, anticipo.getCodigo());
		}

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idAnticipo", anticipo.getCodigo());
		resultado.put("idPago", idPago);
		resultado.put("estadoPago", anticipo.getPagoProgramado() != null
				? anticipo.getPagoProgramado().getEstado() : null);
		resultado.put("numeroCheque", resultadoPago.get("numeroCheque"));
		System.out.println("✓ Anticipo " + anticipo.getCodigo() + " aprobado"
				+ " | idPago=" + idPago + " | estado actual del anticipo=" + anticipo.getEstado());
		return resultado;
	}

	@Override
	public void anular(Long idAnticipo, String motivo, Long idUsuario) throws Throwable {
		System.out.println("=== anular anticipo | idAnticipo=" + idAnticipo + " ===");

		AnticipoEmpleado anticipo = anticipoEmpleadoDaoService.selectById(idAnticipo, NombreEntidadesRhh.ANTICIPO_EMPLEADO);
		if (anticipo == null) {
			throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
		}
		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}

		Long estado = anticipo.getEstado();
		if (Long.valueOf(EstadoAnticipoEmpleado.SOLICITADO).equals(estado)) {
			// Nada más que validar: todavía no hay pago.
		} else if (Long.valueOf(EstadoAnticipoEmpleado.APROBADO).equals(estado)) {
			// Defensivo: hoy inalcanzable en la práctica porque el pago nace
			// CONFIRMADO (sólo admite cheque o débito automático), así que
			// aprobar() nunca deja el anticipo en APROBADO con un pago vivo
			// sin confirmar — se deja por si esa restricción cambia a futuro.
			if (anticipo.getPagoProgramado() != null
					&& Long.valueOf(EstadoPagoProgramado.CONFIRMADO).equals(anticipo.getPagoProgramado().getEstado())) {
				throw new IncomeException("El anticipo " + idAnticipo + " ya tiene el pago confirmado:"
						+ " revierta el pago (POST /pgtr/revertirConfirmado/{id}) antes de anular.");
			}
		} else {
			throw new IncomeException("Sólo se puede anular un anticipo SOLICITADO, o APROBADO sin pago"
					+ " confirmado. Estado actual: " + estado
					+ ". Si ya está PAGADO o EN_DESCUENTO, revierta el pago primero.");
		}

		anticipo.setEstado(Long.valueOf(EstadoAnticipoEmpleado.ANULADO));
		anticipo.setMotivoAnulacion(motivo.trim());
		anticipoEmpleadoDaoService.save(anticipo, anticipo.getCodigo());
		System.out.println("✓ Anticipo " + idAnticipo + " anulado. Motivo: " + motivo);
	}

	@Override
	public List<AnticipoEmpleado> listar(Long idEmpresa, Long idEmpleado, Long estado) throws Throwable {
		return anticipoEmpleadoDaoService.selectListado(idEmpresa, idEmpleado, estado);
	}

	@Override
	public AnticipoEmpleado consultarPorEmpleado(Long idEmpleado) throws Throwable {
		return anticipoEmpleadoDaoService.selectVigenteByEmpleado(idEmpleado);
	}

	// =====================================================================
	// Helpers
	// =====================================================================

	private String nombreCompleto(Empleado empleado) {
		String nombre = (nvl(empleado.getNombres(), "") + " " + nvl(empleado.getApellidos(), "")).trim();
		return nombre.isEmpty() ? ("empleado " + empleado.getCodigo()) : nombre;
	}

	private String nombreUsuario(Long idUsuario) {
		if (idUsuario == null) {
			return "SISTEMA";
		}
		Usuario usuario = em.find(Usuario.class, idUsuario);
		return (usuario != null && usuario.getNombre() != null) ? usuario.getNombre() : "SISTEMA";
	}

	private String nvl(String valor, String porDefecto) {
		return (valor != null && !valor.trim().isEmpty()) ? valor : porDefecto;
	}

}
