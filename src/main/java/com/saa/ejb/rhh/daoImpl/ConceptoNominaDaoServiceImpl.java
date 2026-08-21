package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ConceptoNominaDaoService;
import com.saa.model.rhh.ConceptoNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ConceptoNominaDaoService.
 */
@Stateless
public class ConceptoNominaDaoServiceImpl extends EntityDaoImpl<ConceptoNomina> implements ConceptoNominaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ConceptoNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ConceptoNomina");
		return new String[]{"codigo",
							"empresa",
							"nombre",
							"abreviatura",
							"codigoAlterno",
							"tipoConcepto",
							"tipoCalculo",
							"baseCalculo",
							"tipoRelacionLaboral",
							"rolMotor",
							"valor",
							"porcentaje",
							"formula",
							"imponibleIess",
							"imponibleIr",
							"aportaFondosReserva",
							"baseDecimoTercero",
							"baseDecimoCuarto",
							"baseVacaciones",
							"baseUtilidades",
							"patronal",
							"provision",
							"obligatorio",
							"recortable",
							"casilleroRdep",
							"codigoIess",
							"casilleroF107",
							"planCuenta",
							"detallePlantilla",
							"orden",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ConceptoNominaDaoService#selectByCodigoAlterno(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ConceptoNomina selectByCodigoAlterno(Long codigoAlterno, Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByCodigoAlterno de ConceptoNomina: " + codigoAlterno
				+ ", empresa: " + idEmpresa);
		if (codigoAlterno == null) {
			return null;
		}
		Query query = em.createQuery(" select   t "
				+ " from     ConceptoNomina t "
				+ " where    t.codigoAlterno = :codigoAlterno "
				+ "          and t.empresa.codigo = :idEmpresa ");
		query.setParameter("codigoAlterno", codigoAlterno);
		query.setParameter("idEmpresa", idEmpresa);
		List<ConceptoNomina> encontrados = query.getResultList();
		if (encontrados == null || encontrados.isEmpty()) {
			return null;
		}
		return encontrados.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ConceptoNominaDaoService#selectActivosByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ConceptoNomina> selectActivosByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectActivosByEmpresa de ConceptoNomina, empresa: " + idEmpresa);
		Query query = em.createQuery(" select   t "
				+ " from     ConceptoNomina t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.estado = 1 "
				+ " order by t.orden, t.codigoAlterno ");
		query.setParameter("idEmpresa", idEmpresa);
		return query.getResultList();
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ConceptoNominaDaoService#selectByRolMotor(java.lang.Integer, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ConceptoNomina selectByRolMotor(Integer rolMotor, Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByRolMotor de ConceptoNomina, rol: " + rolMotor
				+ ", empresa: " + idEmpresa);
		if (rolMotor == null) {
			return null;
		}
		Query query = em.createQuery(" select   t "
				+ " from     ConceptoNomina t "
				+ " where    t.rolMotor = :rolMotor "
				+ "          and t.empresa.codigo = :idEmpresa "
				+ "          and t.estado = 1 ");
		query.setParameter("rolMotor", Long.valueOf(rolMotor.longValue()));
		query.setParameter("idEmpresa", idEmpresa);
		List<ConceptoNomina> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}
}
