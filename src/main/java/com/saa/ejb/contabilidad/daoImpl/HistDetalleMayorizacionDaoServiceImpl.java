package com.saa.ejb.contabilidad.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.HistDetalleMayorizacionDaoService;
import com.saa.model.contabilidad.HistDetalleMayorizacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistDetalleMayorizacionDaoServiceImpl extends EntityDaoImpl<HistDetalleMayorizacion>  implements HistDetalleMayorizacionDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"histMayorizacion",
							"planCuenta",
							"saldoAnterior",
							"valorDebe",
							"valorHaber",
							"saldoActual",
							"numeroCuenta",
							"codigoPadreCuenta",
							"nombreCuenta",
							"tipoCuenta",
							"nivelCuenta",
							"idMayorizacion"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.HistDetalleMayorizacionDaoService#deleteByDesmayorizacion(java.lang.Long)
	 */
	public void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByDesmayorizacion con idDesmayorizacion: " + idDesmayorizacion);
		Query query = em.createQuery(" Delete from HistDetalleMayorizacion b" +
									 " where b.histMayorizacion.codigo = :idDesmayorizacion");
		query.setParameter("idDesmayorizacion", idDesmayorizacion);
		query.executeUpdate();
	}
	
}