package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.SalidaOficialDaoService;
import com.saa.model.rhh.SalidaOficial;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion SalidaOficialDaoService.
 */
@Stateless
public class SalidaOficialDaoServiceImpl extends EntityDaoImpl<SalidaOficial>
		implements SalidaOficialDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SalidaOficialDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) SalidaOficial");
		return new String[]{"codigo",
							"empresa",
							"tipoSalida",
							"anio",
							"mes",
							"empleado",
							"rutaArchivo",
							"nombreArchivo",
							"hash",
							"fechaGeneracion",
							"fechaPresentacion",
							"numeroComprobante",
							"observaciones",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SalidaOficialDaoService#selectSalida(java.lang.Long, java.lang.Long, java.lang.Integer, java.lang.Integer, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SalidaOficial selectSalida(Long idEmpresa, Long tipoSalida, Integer anio, Integer mes,
			Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo selectSalida de SalidaOficial, tipo: " + tipoSalida
				+ ", anio: " + anio + ", mes: " + mes + ", empleado: " + idEmpleado);

		// Los nulos se comparan con 'is null', no con igualdad: en JPQL --como en SQL--
		// null = null es desconocido, no verdadero, y la busqueda no encontraria nunca la
		// salida anual ni la consolidada.
		StringBuilder jpql = new StringBuilder(" select t "
				+ " from   SalidaOficial t "
				+ " where  t.empresa.codigo = :idEmpresa "
				+ "        and t.tipoSalida = :tipoSalida "
				+ "        and t.anio = :anio ");
		jpql.append(mes != null ? " and t.mes = :mes " : " and t.mes is null ");
		jpql.append(idEmpleado != null ? " and t.empleado.codigo = :idEmpleado "
				: " and t.empleado is null ");
		jpql.append(" order by t.codigo desc ");

		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("tipoSalida", tipoSalida);
		query.setParameter("anio", anio);
		if (mes != null) {
			query.setParameter("mes", mes);
		}
		if (idEmpleado != null) {
			query.setParameter("idEmpleado", idEmpleado);
		}
		List<SalidaOficial> lista = query.getResultList();
		return lista.isEmpty() ? null : lista.get(0);
	}
}
