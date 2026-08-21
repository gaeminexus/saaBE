/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ReglonNominaDaoService;
import com.saa.model.rhh.ReglonNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ReglonNominaDaoService. 
 */
@Stateless
public class ReglonNominaDaoServiceImpl extends EntityDaoImpl<ReglonNomina>  implements ReglonNominaDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ReglonNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ReglonNomina");
		return new String[]{"codigo",
							"nomina",
							"cantidad",
							"valor",
							"imponible",
							"orden",
							"fechaRegistro",
							"usuarioRegistro",
							"conceptoNomina",
							"descripcion",
							"tipoConcepto",
							"baseCalculo",
							"porcentaje",
							"origen",
							"manual",
							"imponibleIess",
							"gravadoIr",
							"patronal",
							"tablaReferencia",
							"idReferencia"};
	}
	

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ReglonNominaDaoService#selectByNomina(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ReglonNomina> selectByNomina(Long idNomina) throws Throwable {
		System.out.println("Ingresa al metodo selectByNomina de ReglonNomina, nomina: " + idNomina);
		Query query = em.createQuery(" select   t "
				+ " from     ReglonNomina t "
				+ " where    t.nomina.codigo = :idNomina "
				+ " order by t.orden, t.codigo ");
		query.setParameter("idNomina", idNomina);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ReglonNominaDaoService#selectManualesByNomina(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ReglonNomina> selectManualesByNomina(Long idNomina) throws Throwable {
		System.out.println("Ingresa al metodo selectManualesByNomina de ReglonNomina, nomina: " + idNomina);
		Query query = em.createQuery(" select   t "
				+ " from     ReglonNomina t "
				+ " where    t.nomina.codigo = :idNomina "
				+ "          and t.manual = 'S' "
				+ " order by t.orden, t.codigo ");
		query.setParameter("idNomina", idNomina);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ReglonNominaDaoService#eliminaGeneradosByNomina(java.lang.Long)
	 */
	@Override
	public int eliminaGeneradosByNomina(Long idNomina) throws Throwable {
		System.out.println("Ingresa al metodo eliminaGeneradosByNomina de ReglonNomina, nomina: " + idNomina);
		// Los renglones manuales sobreviven al recalculo: es la razon de RNGLMNAL.
		Query query = em.createQuery(" delete from ReglonNomina t "
				+ " where  t.nomina.codigo = :idNomina "
				+ "        and (t.manual is null or t.manual <> 'S') ");
		query.setParameter("idNomina", idNomina);
		return query.executeUpdate();
	}
}
