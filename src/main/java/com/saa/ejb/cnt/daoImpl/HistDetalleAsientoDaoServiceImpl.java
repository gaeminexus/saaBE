package com.saa.ejb.cnt.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.HistDetalleAsientoDaoService;
import com.saa.model.cnt.HistDetalleAsiento;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistDetalleAsientoDaoServiceImpl extends EntityDaoImpl<HistDetalleAsiento>  implements HistDetalleAsientoDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"histAsiento",
							"planCuenta",
							"descripcion",
							"valorDebe",
							"valorHaber",
							"nombreCuenta",
							"centroCosto",
							"numeroCuenta"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.HistDetalleAsientoDaoService#deleteByHistAsiento(java.lang.Long)
	 */
	public void deleteByHistAsiento(Long idHistAsiento) throws Throwable {
		System.out.println("Ingresa al metodo deleteByHistAsiento con idHistAsiento: " + idHistAsiento);
		Query query = em.createQuery(" Delete from HistDetalleAsiento b" +
									 " where b.histAsiento.codigo = :idHistAsiento");
		query.setParameter("idHistAsiento", idHistAsiento);
		query.executeUpdate();
	}
	
}
