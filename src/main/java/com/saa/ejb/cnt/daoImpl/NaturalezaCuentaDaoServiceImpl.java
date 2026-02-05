package com.saa.ejb.cnt.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.NaturalezaCuentaDaoService;
import com.saa.model.cnt.NaturalezaCuenta;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

@Stateless
public class NaturalezaCuentaDaoServiceImpl extends EntityDaoImpl<NaturalezaCuenta>  implements NaturalezaCuentaDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"nombre",
							"tipo",
							"numero",
							"estado",
							"empresa",
							"manejaCentroCosto"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.NaturalezaCuentaDaoService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo deleteByEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" Delete b " +
									 " from   NaturalezaCuenta b " +
									 " where   b.empresa.codigo = :empresa");
		query.setParameter("empresa", empresa);		
		query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.NaturalezaCuentaDaoService#selectByNumeroEmpresa(java.lang.Long, java.lang.Long)
	 */
	public NaturalezaCuenta selectByNumeroEmpresa(Long numero, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByNumeroEmpresa de empresa: " + empresa + ", con numero: " + numero);
		NaturalezaCuenta resultado = null;
		Query query = em.createQuery(" select b " +
									 " from    NaturalezaCuenta b " +
									 " where   b.empresa.codigo = :empresa " +
									 "         and   b.numero = :numero " +
									 "         and   b.estado = :estado");
		query.setParameter("empresa", empresa);		
		query.setParameter("numero", numero);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		try {
			resultado = (NaturalezaCuenta)query.getSingleResult();
		} catch (NonUniqueResultException e) {
			throw new Exception("EXISTE MAS DE UNA NATURALEZA CON GRUPO: " + numero);
		} catch (NoResultException e) {
			throw new Exception("NO EXISTE NATURALEZA CON GRUPO: " + numero);
		} catch (PersistenceException e) {
			throw new Exception("ERROR EN Naturaleza Cuenta-selectByNumeroEmpresa: " + e.getMessage());
		}
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.NaturalezaCuentaDaoService#selectByNumeroEmpresaSinExce(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<NaturalezaCuenta> selectByNumeroEmpresaSinExce(String numero,
			Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByNumeroEmpresa de empresa: " + empresa + ", con numero: " + numero);
		Query query = em.createQuery(" select b " +
									 " from    NaturalezaCuenta b " +
									 " where   b.empresa.codigo = :empresa " +
									 "         and   b.numero = :numero " +
									 "         and   b.estado = :estado");
		query.setParameter("empresa", empresa);		
		query.setParameter("numero", Long.valueOf(numero));
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));

		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.NaturalezaCuentaDaoService#selectByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<NaturalezaCuenta> selectByEmpresa(Long empresa)
			throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" select b " +
								 	 " from    NaturalezaCuenta b " +
									 " where   b.empresa.codigo = :empresa " +
									 " order by b.numero ");									 
		query.setParameter("empresa", empresa);		
		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.NaturalezaCuentaDaoService#selectActivosByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<NaturalezaCuenta> selectActivosByEmpresa(Long empresa)
			throws Throwable {
		System.out.println("Ingresa al metodo selectActivosByEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from    NaturalezaCuenta b " +
									 " where   b.empresa.codigo = :empresa " +
									 "         and   b.estado = :estado");
		query.setParameter("empresa", empresa);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		
		return query.getResultList();
	}
	
}
