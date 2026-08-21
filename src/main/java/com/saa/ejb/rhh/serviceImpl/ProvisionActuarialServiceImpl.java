package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.ProvisionNominaDaoService;
import com.saa.ejb.rhh.service.ProvisionActuarialService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.ConfiguracionNomina;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.ProvisionNomina;
import com.saa.rubros.RhhTipoProvision;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 * <p>Implementacion de ProvisionActuarialService.</p>
 *
 * <p>Estas provisiones no se calculan: se cargan. El valor lo determina un actuario
 * externo y aqui solo se registra, se valida contra la configuracion de la empresa y se
 * deja disponible para el asiento de provisiones.</p>
 *
 * <p><b>La carga es idempotente por (periodo, empleado, tipo)</b>: volver a cargar el
 * estudio actualiza el valor en vez de duplicar la provision, que es el comportamiento util
 * cuando el actuario corrige una cifra.</p>
 */
@Stateless
public class ProvisionActuarialServiceImpl implements ProvisionActuarialService {

	/** Marca de "si" de las banderas S/N del esquema. */
	private static final String SI = "S";

	@PersistenceContext
	private EntityManager em;

	@EJB
	private ProvisionNominaDaoService provisionNominaDaoService;

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	@EJB
	private ConfiguracionNominaDaoService configuracionNominaDaoService;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProvisionActuarialService#cargarProvisionActuarial(java.lang.Long, java.lang.Long, int, java.lang.Double, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ProvisionNomina cargarProvisionActuarial(Long idPeriodoNomina, Long idEmpleado,
			int tipoProvision, Double valor, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo cargarProvisionActuarial, periodo: " + idPeriodoNomina
				+ ", empleado: " + idEmpleado + ", tipo: " + tipoProvision);

		PeriodoNomina periodo = periodoNominaDaoService.selectById(idPeriodoNomina,
				NombreEntidadesRhh.PERIODO_NOMINA);
		if (periodo == null) {
			throw new IncomeException("No existe el periodo de nomina " + idPeriodoNomina + ".");
		}
		Empleado empleado = em.find(Empleado.class, idEmpleado);
		if (empleado == null) {
			throw new IncomeException("No existe el empleado " + idEmpleado + ".");
		}

		exigeProvisionActivada(periodo, tipoProvision);

		ProvisionNomina provision = buscaProvision(idPeriodoNomina, idEmpleado, tipoProvision);
		if (provision == null) {
			provision = new ProvisionNomina();
			provision.setPeriodoNomina(periodo);
			provision.setEmpleado(empleado);
			provision.setTipoProvision(Long.valueOf(tipoProvision));
			provision.setEstado(Long.valueOf(1L));
			provision.setFechaRegistro(LocalDateTime.now());
		}
		// Sin concepto ni base: el valor no se deriva de ninguna base de nomina, viene
		// del estudio actuarial. La cuenta contable sale de la plantilla del asiento.
		provision.setConceptoNomina(null);
		provision.setBaseCalculo(null);
		provision.setValor(RedondeoNomina.redondea(valor));
		provision.setUsuarioRegistro(usuario);
		return provisionNominaDaoService.save(provision, provision.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.ProvisionActuarialService#cargarEstudioActuarial(java.lang.Long, int, java.util.List, java.util.List, java.lang.String)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public int cargarEstudioActuarial(Long idPeriodoNomina, int tipoProvision, List<Long> idsEmpleado,
			List<Double> valores, String usuario) throws Throwable {
		System.out.println("Ingresa al metodo cargarEstudioActuarial, periodo: " + idPeriodoNomina
				+ ", tipo: " + tipoProvision);

		if (idsEmpleado == null || valores == null) {
			throw new IncomeException("El estudio actuarial no trae empleados ni valores.");
		}
		if (idsEmpleado.size() != valores.size()) {
			throw new IncomeException("El estudio actuarial trae " + idsEmpleado.size()
					+ " empleado(s) y " + valores.size() + " valor(es): las dos listas deben"
					+ " tener el mismo tamano y el mismo orden.");
		}

		int cargadas = 0;
		for (int i = 0; i < idsEmpleado.size(); i++) {
			cargarProvisionActuarial(idPeriodoNomina, idsEmpleado.get(i), tipoProvision,
					valores.get(i), usuario);
			cargadas++;
		}
		System.out.println("cargarEstudioActuarial termino: " + cargadas + " provision(es).");
		return cargadas;
	}

	// =====================================================================
	// Apoyo
	// =====================================================================

	/**
	 * Exige que la empresa tenga activada esa provision en su configuracion.
	 *
	 * @param periodo		: Periodo de nomina
	 * @param tipoProvision	: Codigo alterno del detalle del rubro RHH_TIPO_PROVISION
	 * @throws Throwable	: IncomeException si no esta activada o el tipo no es actuarial
	 */
	private void exigeProvisionActivada(PeriodoNomina periodo, int tipoProvision) throws Throwable {
		Long idEmpresa = periodo.getEmpresa() != null ? periodo.getEmpresa().getCodigo() : null;
		ConfiguracionNomina cfnm = configuracionNominaDaoService.selectByEmpresa(idEmpresa);
		if (cfnm == null) {
			throw new IncomeException("La empresa " + idEmpresa + " no tiene configuracion de nomina"
					+ " (RHH.CFNM). Ejecute el script 07 antes de cargar provisiones actuariales.");
		}

		if (tipoProvision == RhhTipoProvision.JUBILACION_PATRONAL) {
			if (!SI.equals(cfnm.getAplicaJubilacionPatronal())) {
				throw new IncomeException("La empresa no tiene activada la provision de jubilacion"
						+ " patronal (CFNMAPJP = 'N'). Actívela en la configuracion de nomina antes"
						+ " de cargar el estudio actuarial.");
			}
			return;
		}
		if (tipoProvision == RhhTipoProvision.DESAHUCIO) {
			if (!SI.equals(cfnm.getAplicaDesahucio())) {
				throw new IncomeException("La empresa no tiene activada la provision de desahucio"
						+ " (CFNMAPDS = 'N'). Actívela en la configuracion de nomina antes de"
						+ " cargar el estudio actuarial.");
			}
			return;
		}
		throw new IncomeException("El tipo de provision " + tipoProvision + " no es actuarial."
				+ " Solo la jubilacion patronal y el desahucio se cargan desde un estudio externo;"
				+ " las demas las genera calcularPeriodo a partir de las bases del periodo.");
	}

	/**
	 * Busca la provision de un empleado, periodo y tipo, para hacer idempotente la carga.
	 *
	 * @param idPeriodoNomina	: Id del periodo
	 * @param idEmpleado		: Id del empleado
	 * @param tipoProvision		: Codigo alterno del detalle del rubro RHH_TIPO_PROVISION
	 * @return					: La provision, o null si aun no existe
	 * @throws Throwable		: Excepcion
	 */
	@SuppressWarnings("unchecked")
	private ProvisionNomina buscaProvision(Long idPeriodoNomina, Long idEmpleado,
			int tipoProvision) throws Throwable {
		Query query = em.createQuery(" select   t "
				+ " from     ProvisionNomina t "
				+ " where    t.periodoNomina.codigo = :idPeriodo "
				+ "          and t.empleado.codigo = :idEmpleado "
				+ "          and t.tipoProvision = :tipoProvision ");
		query.setParameter("idPeriodo", idPeriodoNomina);
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("tipoProvision", Long.valueOf(tipoProvision));
		List<ProvisionNomina> encontradas = query.getResultList();
		return (encontradas == null || encontradas.isEmpty()) ? null : encontradas.get(0);
	}
}
