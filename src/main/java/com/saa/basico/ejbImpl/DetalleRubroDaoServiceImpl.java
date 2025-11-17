/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.ejbImpl;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.model.scp.DetalleRubro;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.Rubros;

/**
 * @author GaemiSoft.
 *         Clase DetalleRubroDaoImpl.
 */
@Stateless
public class DetalleRubroDaoServiceImpl extends EntityDaoImpl<DetalleRubro> implements DetalleRubroDaoService {

	// Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.dao.DetalleRubroDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[] { "codigo",
				"rubro",
				"descripcion",
				"valorNumerico",
				"valorAlfanumerico",
				"codigoAlterno",
				"estado" };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.sistema.ejb.dao.DetalleRubroDaoService#
	 * selectValorStringByRubAltDetAlt(int, int)
	 */
	public String selectValorStringByRubAltDetAlt(int codigoAlternoRubro,
			int codigoAlternoDetalle) throws Throwable {
		/*
		 * System.out.
		 * println("DetalleRubro.selectValorStringByRubAltDetAlt con codigoAlternoRubro: "
		 * + codigoAlternoRubro
		 * + " y codigoAlternoDetalle: " + codigoAlternoDetalle);
		 */
		Query query = em.createQuery(" select   t.valorAlfanumerico " +
				" from     DetalleRubro t " +
				" where    t.rubro.codigoAlterno = :codigoAlternoRubro " +
				" 			and   t.codigoAlterno = :codigoAlternoDetalle ");
		query.setParameter("codigoAlternoRubro", Long.valueOf(codigoAlternoRubro));
		query.setParameter("codigoAlternoDetalle", Long.valueOf(codigoAlternoDetalle));

		return (String) query.getSingleResult();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.sistema.ejb.dao.DetalleRubroDaoService#
	 * selectModulosNoClienteConContabilidad()
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleRubro> selectModulosNoClienteConContabilidad()
			throws Throwable {
		// System.out.println("DetalleRubro.selectModulosNoClienteConContabilidad");
		Query query = em.createQuery(" from     DetalleRubro t " +
				" where    t.rubro.codigoAlterno = :modulosSistema " +
				" 			and   t.codigoAlterno NOT IN (select   b.codigoAlterno" +
				"										  from     DetalleRubro b" +
				"										  where    b.rubro.codigoAlterno = :modulosCliente" +
				"										           and   b.codigoAlterno != :moduloContabilidad)");
		query.setParameter("modulosSistema", Long.valueOf(Rubros.MODULO_SISTEMA));
		query.setParameter("modulosCliente", Long.valueOf(Rubros.MODULO_CLIENTE));
		query.setParameter("moduloContabilidad", Long.valueOf(ModuloSistema.CONTABILIDAD));

		return query.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.sistema.ejb.dao.DetalleRubroDaoService#
	 * selectByCodigoAlternoRubro(int)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleRubro> selectByCodigoAlternoRubro(int codigoAlternoRubro, Long estado)
			throws Throwable {
		// System.out.println("DetalleRubro.selectByCodigoAlternoRubro con
		// codigoAlternoRubro: " + codigoAlternoRubro);
		Query query = em.createQuery(" from     DetalleRubro t " +
				" where    t.rubro.codigoAlterno = :codigoAlternoRubro " +
				"          and   t.estado = :estado");
		query.setParameter("codigoAlternoRubro", Long.valueOf(codigoAlternoRubro));
		query.setParameter("estado", estado);
		return query.getResultList();
	}

	/*
	 * DESDE AQUI COMIENZAN LOS METODOS DE LA INTERFAZ ENTITY DAO
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.tesoreria.ejb.util.EntityDao#selectAll(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleRubro> selectAll(String entidad) throws Throwable {
		// System.out.println("Entidad.recuperaTodos");
		Query query = em.createNamedQuery(entidad + "All");
		return query.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.saa.basico.ejb.DetalleRubroDaoService#save(com.gaemisof.scp.
	 * model.DetalleRubro)
	 */
	public DetalleRubro save(DetalleRubro tipo) throws Throwable {
		System.out.println("IngresaSave - entidad: " + tipo.getClass());
		em.persist(tipo);
		return tipo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.tesoreria.ejb.util.EntityDao#find(java.lang.Object,
	 * java.lang.Long)
	 */
	public DetalleRubro find(DetalleRubro clase, Long id) throws Throwable {
		return (DetalleRubro) em.find(clase.getClass(), id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.tesoreria.ejb.util.EntityDao#remove(java.lang.Object,
	 * java.lang.Long)
	 */
	public void remove(DetalleRubro clase, Long id) throws Throwable {
		DetalleRubro tipo = (DetalleRubro) find(clase, id);
		if (tipo != null) {
			em.remove(tipo);
			try {
				em.flush();
			} catch (PersistenceException e) {
				System.out.println("Error al eliminar:" + e.getCause().getCause().getMessage());
				throw e.getCause().getCause();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.util.EntityDao#selectById(java.lang.Long,
	 * java.lang.String)
	 */
	public DetalleRubro selectById(Long id, String entidad) throws Throwable {
		System.out.println("Ingresa al metodo selectById con entidad: " + entidad + " y id: " + id);
		Query query = em.createNamedQuery(entidad + "Id");
		query.setParameter("id", id);
		return (DetalleRubro) query.getSingleResult();
	}

	public String selectDescripcionByRubAltDetAlt(int codigoAlternoRubro,
			int codigoAlternoDetalle) throws Throwable {
		/*
		 * System.out.
		 * println("DetalleRubro.selectValorStringByRubAltDetAlt con codigoAlternoRubro: "
		 * + codigoAlternoRubro
		 * + " y codigoAlternoDetalle: " + codigoAlternoDetalle);
		 */
		Query query = em.createQuery(" select   t.descripcion " +
				" from     DetalleRubro t " +
				" where    t.rubro.codigoAlterno = :codigoAlternoRubro " +
				" 			and   t.codigoAlterno = :codigoAlternoDetalle ");
		query.setParameter("codigoAlternoRubro", Long.valueOf(codigoAlternoRubro));
		query.setParameter("codigoAlternoDetalle", Long.valueOf(codigoAlternoDetalle));

		return (String) query.getSingleResult();
	}

	public DetalleRubro save(DetalleRubro tipo, Long id) throws Throwable {
		System.out.println("IngresaSave - entidad: " + tipo.getClass());
		em.persist(tipo);
		return tipo;
	}

	@Override
	public Double selectValorNumericoByRubAltDetAlt(int codigoAlternoRubro, int codigoAlternoDetalle) throws Throwable {
		System.out.println("selectValorNumericoByRubAltDetAlt con codigoAlternoRubro: " + codigoAlternoRubro
				+ " y codigoAlternoDetalle: " + codigoAlternoDetalle);
		Query query = em.createQuery(" select   t.valorNumerico " +
				" from     DetalleRubro t " +
				" where    t.rubro.codigoAlterno = :codigoAlternoRubro " +
				" 			and   t.codigoAlterno = :codigoAlternoDetalle ");
		query.setParameter("codigoAlternoRubro", Long.valueOf(codigoAlternoRubro));
		query.setParameter("codigoAlternoDetalle", Long.valueOf(codigoAlternoDetalle));
		return (Double) query.getSingleResult();
	}

}
