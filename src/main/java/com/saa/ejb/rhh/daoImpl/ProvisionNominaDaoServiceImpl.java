package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ProvisionNominaDaoService;
import com.saa.model.rhh.ProvisionNomina;
import com.saa.rubros.RhhModoPeriodoNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ProvisionNominaDaoService.
 */
@Stateless
public class ProvisionNominaDaoServiceImpl extends EntityDaoImpl<ProvisionNomina> implements ProvisionNominaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ProvisionNomina");
		return new String[]{"codigo",
							"periodoNomina",
							"empleado",
							"conceptoNomina",
							"tipoProvision",
							"baseCalculo",
							"valor",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#eliminaByPeriodo(java.lang.Long)
	 */
	@Override
	public int eliminaByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByPeriodo de ProvisionNomina, periodo: " + idPeriodo);
		Query query = em.createQuery(" delete from ProvisionNomina t "
				+ " where  t.periodoNomina.codigo = :idPeriodo ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#eliminaByPeriodoYEmpleado(java.lang.Long, java.lang.Long)
	 */
	@Override
	public int eliminaByPeriodoYEmpleado(Long idPeriodo, Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByPeriodoYEmpleado de ProvisionNomina, periodo: "
				+ idPeriodo + ", empleado: " + idEmpleado);
		Query query = em.createQuery(" delete from ProvisionNomina t "
				+ " where  t.periodoNomina.codigo = :idPeriodo "
				+ "        and t.empleado.codigo = :idEmpleado ");
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("idEmpleado", idEmpleado);
		return query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#selectByPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ProvisionNomina> selectByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodo de ProvisionNomina, periodo: " + idPeriodo);
		Query query = em.createQuery(" select   t "
				+ " from     ProvisionNomina t "
				+ " where    t.periodoNomina.codigo = :idPeriodo "
				+ " order by t.tipoProvision, t.empleado.apellidos ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#sumaValorByEmpleadoYTipo(java.lang.Long, java.lang.Long)
	 */
	@Override
	public Double sumaValorByEmpleadoYTipo(Long idEmpleado, Long tipoProvision) throws Throwable {
		System.out.println("Ingresa al metodo sumaValorByEmpleadoYTipo de ProvisionNomina, empleado: "
				+ idEmpleado + ", tipo: " + tipoProvision);
		// Excluye periodos historicos (2026-09-01, #4bis del plan): generaProvision no esta
		// gateado por modo historico y SI escribe filas reales en periodos que despues nunca
		// generan asiento (contabilizarProvisiones si corta ahi). Sumar esas filas convertiria
		// PVNM en el devengo operativo en vez del saldo contable real. modo = PRODUCTIVO es el
		// complemento exacto de esHistorico (que trata modo null O historico como historico),
		// asi que exigir el valor productivo excluye las dos formas de "no contabilizado" con
		// una sola comparacion.
		Query query = em.createQuery(" select   sum(t.valor) "
				+ " from     ProvisionNomina t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.tipoProvision = :tipoProvision "
				+ "          and t.periodoNomina.modo = :productivo ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("tipoProvision", tipoProvision);
		query.setParameter("productivo", Long.valueOf(RhhModoPeriodoNomina.PRODUCTIVO_CONTABILIZA));
		Object resultado = query.getSingleResult();
		return resultado == null ? Double.valueOf(0D) : Double.valueOf(resultado.toString());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProvisionNominaDaoService#sumaValorByEmpleadosYTipo(java.util.List, java.lang.Long)
	 */
	@Override
	public Double sumaValorByEmpleadosYTipo(List<Long> idsEmpleados, Long tipoProvision) throws Throwable {
		System.out.println("Ingresa al metodo sumaValorByEmpleadosYTipo de ProvisionNomina, "
				+ (idsEmpleados != null ? idsEmpleados.size() : 0) + " empleado(s), tipo: " + tipoProvision);
		if (idsEmpleados == null || idsEmpleados.isEmpty()) {
			return Double.valueOf(0D);
		}
		// Mismo criterio que sumaValorByEmpleadoYTipo: excluye periodos historicos.
		Query query = em.createQuery(" select   sum(t.valor) "
				+ " from     ProvisionNomina t "
				+ " where    t.empleado.codigo in (:idsEmpleados) "
				+ "          and t.tipoProvision = :tipoProvision "
				+ "          and t.periodoNomina.modo = :productivo ");
		query.setParameter("idsEmpleados", idsEmpleados);
		query.setParameter("tipoProvision", tipoProvision);
		query.setParameter("productivo", Long.valueOf(RhhModoPeriodoNomina.PRODUCTIVO_CONTABILIZA));
		Object resultado = query.getSingleResult();
		return resultado == null ? Double.valueOf(0D) : Double.valueOf(resultado.toString());
	}
}
