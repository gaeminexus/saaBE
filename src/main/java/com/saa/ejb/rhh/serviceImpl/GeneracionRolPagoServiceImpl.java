package com.saa.ejb.rhh.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.ReglonNominaDaoService;
import com.saa.ejb.rhh.dao.RolPagoDaoService;
import com.saa.ejb.rhh.service.GeneracionRolPagoService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Nomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.ReglonNomina;
import com.saa.model.rhh.RolPago;
import com.saa.rubros.RhhEstadoPeriodoNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Implementacion de GeneracionRolPagoService.</p>
 *
 * <h3>El numero del rol</h3>
 *
 * <p><code>RLPGNMRO</code> se arma con el periodo y el empleado --<code>AAAAMM-NNNNNN</code>--,
 * de modo que es unico y ademas legible: quien ve el numero sabe de que mes y de quien es.
 * Al ser determinista, regenerar el rol produce el mismo numero, que es lo que permite que
 * la regeneracion actualice la fila en vez de crear otra con numeracion nueva.</p>
 *
 * <h3>Las tres columnas obligatorias</h3>
 *
 * <p>El barrido de <code>NOT NULL</code> del 2026-08-19 encontro que <code>RLPGESTD</code>,
 * <code>RLPGNMRO</code> y <code>RLPGFCHA</code> son obligatorias, declaradas como CHECK con
 * nombre de sistema. Las tres se llenan siempre; dejarlas al criterio del llamador habria
 * reproducido el <code>ORA-02290</code> que detuvo tres veces la prueba de enero.</p>
 *
 * <p><code>RLPGESTD</code> se queda como <code>String</code> y sin rubro, por la misma
 * decision que se tomo con <code>CNTEESTD</code>: el estado real del rol lo llevan
 * <code>RLPGFCEN</code> (enviado) y <code>RLPGRCBD</code> (recibido), que es lo que la
 * operacion consulta. El valor que se graba es el marcador tecnico A, el mismo que usan
 * las tablas de RHH que conservan estado <code>VARCHAR2</code> --CRGO, DPRT, DPTC, TPCE,
 * TRNO--. No es un valor normativo: no describe ninguna regla de negocio, solo marca la
 * fila como vigente.</p>
 */
@Stateless
public class GeneracionRolPagoServiceImpl implements GeneracionRolPagoService {

	/** Marcador tecnico de fila vigente. Convencion de las tablas de RHH con estado VARCHAR2. */
	private static final String ESTADO_VIGENTE = "A";

	/** Bandera de rol ya entregado al empleado. */
	private static final String SI = "S";

	/** Bandera de rol todavia no entregado. */
	private static final String NO = "N";

	/** Algoritmo del hash de integridad del rol. */
	private static final String ALGORITMO_HASH = "SHA-256";

	/** Separador de campos dentro de la cadena que se resume. */
	private static final String SEPARADOR = "|";

	@EJB
	private RolPagoDaoService rolPagoDaoService;

	@EJB
	private NominaDaoService nominaDaoService;

	@EJB
	private ReglonNominaDaoService reglonNominaDaoService;

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.GeneracionRolPagoService#generarRoles(java.lang.Long, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int generarRoles(Long idPeriodoNomina, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo generarRoles, periodo: " + idPeriodoNomina);
		PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
		exigeEstadoEmitible(periodo);

		List<Nomina> nominas = nominaDaoService.selectByPeriodo(idPeriodoNomina);
		if (nominas == null || nominas.isEmpty()) {
			throw new IncomeException("El periodo " + idPeriodoNomina
					+ " no tiene ninguna nomina calculada: no hay rol que emitir.");
		}

		int generados = 0;
		for (Nomina nomina : nominas) {
			List<ReglonNomina> renglones = reglonNominaDaoService.selectByNomina(nomina.getCodigo());

			RolPago rol = rolPagoDaoService.selectByNomina(nomina.getCodigo());
			if (rol == null) {
				rol = new RolPago();
				rol.setNomina(nomina);
				rol.setFechaRegistro(LocalDate.now());
				rol.setUsuarioRegistro(usuario);
				// La entrega arranca en N. Al regenerar no se vuelve a tocar.
				rol.setRecibido(NO);
			}

			rol.setNumero(armaNumero(periodo, nomina));
			rol.setFechaEmision(LocalDate.now());
			rol.setEstado(ESTADO_VIGENTE);
			rol.setTotalIngresos(nomina.getTotalIngresos());
			rol.setTotalDescuentos(nomina.getTotalDescuentos());
			rol.setNeto(nomina.getNetoPagar());
			rol.setHash(calculaHash(nomina, renglones));

			// RLPGFCEN y RLPGRCBD NO se tocan al regenerar. La entrega al empleado es un
			// hecho ocurrido y volver a emitir el documento no lo deshace. Es el mismo
			// criterio con el que BeneficioSocialService no toca LQBSVLPG al regenerar.

			rolPagoDaoService.save(rol, rol.getCodigo());
			generados++;
		}

		System.out.println("generarRoles emitio o actualizo " + generados
				+ " rol(es) de pago del periodo " + idPeriodoNomina);
		return generados;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.GeneracionRolPagoService#verificarIntegridad(java.lang.Long)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean verificarIntegridad(Long idRolPago) throws Throwable {
		System.out.println("Ingresa al metodo verificarIntegridad, rol: " + idRolPago);
		RolPago rol = rolPagoDaoService.selectById(idRolPago, NombreEntidadesRhh.ROL_PAGO);
		if (rol == null) {
			throw new IncomeException("No existe el rol de pago " + idRolPago + ".");
		}
		if (rol.getHash() == null || rol.getHash().trim().isEmpty()) {
			// Un rol sin hash no se puede declarar integro: devolver true seria afirmar
			// algo que no se comprobo.
			System.out.println("El rol " + idRolPago + " no tiene hash grabado: no se puede verificar.");
			return false;
		}
		Nomina nomina = rol.getNomina();
		List<ReglonNomina> renglones = reglonNominaDaoService.selectByNomina(nomina.getCodigo());
		String recalculado = calculaHash(nomina, renglones);
		boolean integro = rol.getHash().equals(recalculado);
		if (!integro) {
			System.out.println("El rol " + idRolPago + " NO coincide con la nomina actual. Grabado: "
					+ rol.getHash() + " recalculado: " + recalculado);
		}
		return integro;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.GeneracionRolPagoService#registrarRecepcion(java.util.List, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int registrarRecepcion(List<Long> idsRolPago, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo registrarRecepcion, roles: "
				+ (idsRolPago != null ? idsRolPago.size() : 0));
		if (idsRolPago == null || idsRolPago.isEmpty()) {
			throw new IncomeException("No se indico ningun rol de pago que marcar como entregado.");
		}
		int marcados = 0;
		for (Long idRolPago : idsRolPago) {
			RolPago rol = rolPagoDaoService.selectById(idRolPago, NombreEntidadesRhh.ROL_PAGO);
			if (rol == null) {
				// Se aborta la tanda entera: dejarla a medias es peor, porque el operador
				// no sabria cuales quedaron marcados y cuales no.
				throw new IncomeException("No existe el rol de pago " + idRolPago
						+ ". No se marco ninguno de la tanda.");
			}
			// La fecha solo se sella si esta vacia: una entrega ya registrada conserva su
			// fecha original aunque alguien vuelva a enviar el id.
			if (rol.getFechaEnvio() == null) {
				rol.setFechaEnvio(LocalDate.now());
			}
			rol.setRecibido(SI);
			rol.setUsuarioRegistro(usuario);
			rolPagoDaoService.save(rol, rol.getCodigo());
			marcados++;
		}
		System.out.println("registrarRecepcion marco " + marcados + " rol(es) como entregados.");
		return marcados;
	}

	// =====================================================================
	// Piezas
	// =====================================================================

	/**
	 * Recupera el periodo y falla con un mensaje util si no existe.
	 *
	 * @param idPeriodoNomina	: Id del periodo
	 * @return					: El periodo
	 * @throws Throwable		: IncomeException si no existe
	 */
	private PeriodoNomina recuperaPeriodo(Long idPeriodoNomina) throws Throwable {
		if (idPeriodoNomina == null) {
			throw new IncomeException("No se indico el periodo de nomina.");
		}
		PeriodoNomina periodo = periodoNominaDaoService.selectById(idPeriodoNomina,
				NombreEntidadesRhh.PERIODO_NOMINA);
		if (periodo == null) {
			throw new IncomeException("No existe el periodo de nomina " + idPeriodoNomina + ".");
		}
		return periodo;
	}

	/**
	 * El rol solo se emite sobre un periodo aprobado o mas adelante en el flujo.
	 *
	 * <p>Antes de APROBADO el calculo aun puede cambiar, y un rol impreso quedaria
	 * desmentido sin aviso. Se admite CONTABILIZADO y PAGADO porque la regeneracion sigue
	 * teniendo sentido mientras el periodo no se cierre; CERRADO y los estados previos a la
	 * aprobacion se rechazan.</p>
	 *
	 * @param periodo		: Periodo a comprobar
	 * @throws Throwable	: IncomeException si el estado no lo admite
	 */
	private void exigeEstadoEmitible(PeriodoNomina periodo) throws Throwable {
		Long estado = periodo.getEstado();
		if (estado == null) {
			throw new IncomeException("El periodo " + periodo.getCodigo() + " no tiene estado.");
		}
		boolean emitible = Long.valueOf(RhhEstadoPeriodoNomina.APROBADO).equals(estado)
				|| Long.valueOf(RhhEstadoPeriodoNomina.CONTABILIZADO).equals(estado)
				|| Long.valueOf(RhhEstadoPeriodoNomina.PAGADO).equals(estado);
		if (!emitible) {
			throw new IncomeException("Los roles de pago se emiten sobre un periodo APROBADO ("
					+ RhhEstadoPeriodoNomina.APROBADO + "), CONTABILIZADO ("
					+ RhhEstadoPeriodoNomina.CONTABILIZADO + ") o PAGADO ("
					+ RhhEstadoPeriodoNomina.PAGADO + "). El periodo esta en estado " + estado
					+ " y no los admite: antes de aprobar el calculo todavia puede cambiar, y desde"
					+ " CERRADO el periodo ya no se recalcula.");
		}
	}

	/**
	 * Numero del rol: AAAAMM-NNNNNN, con el ano y el mes del periodo y el codigo del
	 * empleado. Determinista, para que regenerar no cambie el numero.
	 *
	 * @param periodo	: Periodo de nomina
	 * @param nomina	: Nomina del empleado
	 * @return			: El numero del rol
	 */
	private String armaNumero(PeriodoNomina periodo, Nomina nomina) {
		Integer anio = periodo.getAnio();
		Integer mes = periodo.getMes();
		Long idEmpleado = nomina.getEmpleado() != null ? nomina.getEmpleado().getCodigo() : null;
		return String.format("%04d%02d-%06d",
				Integer.valueOf(anio != null ? anio.intValue() : 0),
				Integer.valueOf(mes != null ? mes.intValue() : 0),
				Long.valueOf(idEmpleado != null ? idEmpleado.longValue() : 0L));
	}

	/**
	 * SHA-256 del contenido del rol, en un orden determinista.
	 *
	 * <p>Entra lo que el empleado ve firmado: empleado, periodo, dias trabajados, cada
	 * renglon con su concepto y su valor en el orden de presentacion, y los tres totales.
	 * <b>No entra la fecha de emision</b>, ni el usuario, ni la entrega: son metadatos del
	 * documento y no su contenido. Si entraran, el hash cambiaria al regenerar un rol
	 * identico y la verificacion dejaria de significar lo que significa.</p>
	 *
	 * @param nomina		: Nomina del empleado
	 * @param renglones		: Renglones de esa nomina, ya ordenados
	 * @return				: El hash en hexadecimal minusculo
	 * @throws Throwable	: Excepcion
	 */
	private String calculaHash(Nomina nomina, List<ReglonNomina> renglones) throws Throwable {
		StringBuilder contenido = new StringBuilder();
		contenido.append(nomina.getEmpleado() != null ? nomina.getEmpleado().getCodigo() : null).append(SEPARADOR);
		contenido.append(nomina.getPeriodoNomina() != null ? nomina.getPeriodoNomina().getCodigo() : null).append(SEPARADOR);
		contenido.append(nomina.getDiasTrabajados()).append(SEPARADOR);

		if (renglones != null) {
			for (ReglonNomina renglon : renglones) {
				contenido.append(renglon.getOrden()).append(SEPARADOR);
				contenido.append(renglon.getConceptoNomina() != null
						? renglon.getConceptoNomina().getCodigo() : null).append(SEPARADOR);
				contenido.append(renglon.getTipoConcepto()).append(SEPARADOR);
				contenido.append(renglon.getValor()).append(SEPARADOR);
			}
		}

		contenido.append(nomina.getTotalIngresos()).append(SEPARADOR);
		contenido.append(nomina.getTotalDescuentos()).append(SEPARADOR);
		contenido.append(nomina.getNetoPagar());

		MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
		byte[] resumen = digest.digest(contenido.toString().getBytes(StandardCharsets.UTF_8));
		StringBuilder hexadecimal = new StringBuilder(resumen.length * 2);
		for (byte b : resumen) {
			String hex = Integer.toHexString(0xFF & b);
			if (hex.length() == 1) {
				hexadecimal.append('0');
			}
			hexadecimal.append(hex);
		}
		return hexadecimal.toString();
	}

}
