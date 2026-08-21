package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.model.rhh.ParametroNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ParametroNominaDaoService.
 */
@Stateless
public class ParametroNominaDaoServiceImpl extends EntityDaoImpl<ParametroNomina> implements ParametroNominaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ParametroNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ParametroNomina");
		return new String[]{"codigo",
							"empresa",
							"anio",
							"sbu",
							"canastaBasica",
							"aportePersonal",
							"aportePatronal",
							"iece",
							"secap",
							"fondosReserva",
							"porcentajeGastosPersonales",
							"canastasCatastrofica",
							"utilidadPorcentaje",
							"utilidadDias",
							"utilidadCargas",
							"utilidadTopeSbu",
							"diasMes",
							"diasAnio",
							"horasMes",
							"horasDia",
							"recargoSuplementaria",
							"recargoExtraordinaria",
							"recargoNocturno",
							"horaInicioNocturna",
							"horaFinNocturna",
							"maxHorasDia",
							"maxHorasSemana",
							"diasVacaciones",
							"anioVacacionAdicional",
							"maxDiasVacaciones",
							"aniosCaducidadVacaciones",
							"porcentajeDesahucio",
							"indemnizacionMinima",
							"indemnizacionMaxima",
							"aniosIndemnizacionMinima",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ParametroNominaDaoService#selectByAnio(java.lang.Long, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ParametroNomina selectByAnio(Long idEmpresa, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo selectByAnio de ParametroNomina, empresa: " + idEmpresa
				+ ", anio: " + anio);
		Query query = em.createQuery(" select   t "
				+ " from     ParametroNomina t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.anio = :anio "
				+ "          and t.estado = 1 ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("anio", anio);
		List<ParametroNomina> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}
}
