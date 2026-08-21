package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.model.rhh.AcumuladoNomina;
import com.saa.model.rhh.Empleado;
import com.saa.rubros.RhhTipoAcumulado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion AcumuladoNominaDaoService.
 */
@Stateless
public class AcumuladoNominaDaoServiceImpl extends EntityDaoImpl<AcumuladoNomina> implements AcumuladoNominaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.AcumuladoNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) AcumuladoNomina");
		return new String[]{"codigo",
							"empleado",
							"periodoNomina",
							"anio",
							"mes",
							"tipoAcumulado",
							"valor",
							"dias",
							"aperturaMigracion",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.AcumuladoNominaDaoService#sumaValorRango(java.lang.Long, java.lang.Long, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public Double sumaValorRango(Long idEmpleado, Long tipoAcumulado, Integer anioDesde, Integer mesDesde,
			Integer anioHasta, Integer mesHasta) throws Throwable {
		System.out.println("Ingresa al metodo sumaValorRango de AcumuladoNomina, empleado: " + idEmpleado);
		// Se compara anio*100+mes para cubrir periodos que cruzan el cambio de anio,
		// como el del decimo tercero, que va del 1 de diciembre al 30 de noviembre.
		Query query = em.createQuery(" select   sum(t.valor) "
				+ " from     AcumuladoNomina t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.tipoAcumulado = :tipoAcumulado "
				+ "          and ((t.anio * 100) + t.mes) between :claveDesde and :claveHasta "
				+ "          and t.estado = 1 ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("tipoAcumulado", tipoAcumulado);
		query.setParameter("claveDesde", Integer.valueOf(anioDesde.intValue() * 100 + mesDesde.intValue()));
		query.setParameter("claveHasta", Integer.valueOf(anioHasta.intValue() * 100 + mesHasta.intValue()));
		Object resultado = query.getSingleResult();
		return resultado == null ? Double.valueOf(0D) : Double.valueOf(resultado.toString());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.AcumuladoNominaDaoService#sumaDiasRango(java.lang.Long, java.lang.Long, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public Double sumaDiasRango(Long idEmpleado, Long tipoAcumulado, Integer anioDesde, Integer mesDesde,
			Integer anioHasta, Integer mesHasta) throws Throwable {
		System.out.println("Ingresa al metodo sumaDiasRango de AcumuladoNomina, empleado: " + idEmpleado);
		// Misma comparacion por anio*100+mes que sumaValorRango, y la misma razon: los
		// periodos del decimo tercero y del cuarto cruzan el cambio de anio.
		Query query = em.createQuery(" select   sum(t.dias) "
				+ " from     AcumuladoNomina t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.tipoAcumulado = :tipoAcumulado "
				+ "          and ((t.anio * 100) + t.mes) between :claveDesde and :claveHasta "
				+ "          and t.estado = 1 ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("tipoAcumulado", tipoAcumulado);
		query.setParameter("claveDesde", Integer.valueOf(anioDesde.intValue() * 100 + mesDesde.intValue()));
		query.setParameter("claveHasta", Integer.valueOf(anioHasta.intValue() * 100 + mesHasta.intValue()));
		Object resultado = query.getSingleResult();
		return resultado == null ? Double.valueOf(0D) : Double.valueOf(resultado.toString());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.AcumuladoNominaDaoService#selectByClave(java.lang.Long, java.lang.Integer, java.lang.Integer, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AcumuladoNomina selectByClave(Long idEmpleado, Integer anio, Integer mes,
			Long tipoAcumulado) throws Throwable {
		System.out.println("Ingresa al metodo selectByClave de AcumuladoNomina, empleado: " + idEmpleado);
		Query query = em.createQuery(" select   t "
				+ " from     AcumuladoNomina t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.anio = :anio "
				+ "          and t.mes = :mes "
				+ "          and t.tipoAcumulado = :tipoAcumulado ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("anio", anio);
		query.setParameter("mes", mes);
		query.setParameter("tipoAcumulado", tipoAcumulado);
		List<AcumuladoNomina> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.AcumuladoNominaDaoService#eliminaByPeriodo(java.lang.Long)
	 */
	@Override
	public int eliminaByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByPeriodo de AcumuladoNomina, periodo: " + idPeriodo);
		Query query = em.createQuery(" delete from AcumuladoNomina t "
				+ " where  t.periodoNomina.codigo = :idPeriodo ");
		query.setParameter("idPeriodo", idPeriodo);
		return query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.AcumuladoNominaDaoService#sumaValor(java.lang.Long, java.lang.Integer, java.lang.Long, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public Double sumaValor(Long idEmpleado, Integer anio, Long tipoAcumulado, Integer mesDesde,
			Integer mesHasta) throws Throwable {
		System.out.println("Ingresa al metodo sumaValor de AcumuladoNomina, empleado: " + idEmpleado
				+ ", anio: " + anio + ", tipo: " + tipoAcumulado);
		// Un rango invertido (por ejemplo mesDesde 1 y mesHasta 0, que es lo que llega al
		// proyectar desde enero) no tiene meses que sumar: cero, sin ir a la base.
		if (mesDesde == null || mesHasta == null || mesDesde.intValue() > mesHasta.intValue()) {
			return Double.valueOf(0D);
		}
		Query query = em.createQuery(" select   sum(t.valor) "
				+ " from     AcumuladoNomina t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.anio = :anio "
				+ "          and t.tipoAcumulado = :tipoAcumulado "
				+ "          and t.mes between :mesDesde and :mesHasta "
				+ "          and t.estado = 1 ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("anio", anio);
		query.setParameter("tipoAcumulado", tipoAcumulado);
		query.setParameter("mesDesde", mesDesde);
		query.setParameter("mesHasta", mesHasta);
		Object resultado = query.getSingleResult();
		return resultado == null ? Double.valueOf(0D) : Double.valueOf(resultado.toString());
	}
	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.AcumuladoNominaDaoService#selectEmpleadosConAcumuladoEnAnio(java.lang.Long, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Empleado> selectEmpleadosConAcumuladoEnAnio(Long idEmpresa, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo selectEmpleadosConAcumuladoEnAnio de AcumuladoNomina, anio: " + anio);
		// Distinct sobre el empleado, no sobre el acumulado: cada persona tiene varias filas
		// al anio y aqui interesa la lista de a quien declarar.
		Query query = em.createQuery(" select   distinct t.empleado "
				+ " from     AcumuladoNomina t "
				+ " where    t.empleado.empresa.codigo = :idEmpresa "
				+ "          and t.anio = :anio "
				+ "          and t.tipoAcumulado in :tipos "
				+ "          and t.estado = 1 "
				+ "          and t.valor is not null and t.valor <> 0 "
				+ " order by t.empleado.identificacion ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("anio", anio);
		query.setParameter("tipos", List.of(Long.valueOf(RhhTipoAcumulado.GRAVADO_IR),
				Long.valueOf(RhhTipoAcumulado.RETENCION_IR)));
		return query.getResultList();
	}

}
