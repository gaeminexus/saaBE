package com.saa.ejb.cnt.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.DetalleMayorAnaliticoDaoService;
import com.saa.model.cnt.DetalleMayorAnalitico;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetalleMayorAnaliticoDaoServiceImpl extends EntityDaoImpl<DetalleMayorAnalitico>  implements DetalleMayorAnaliticoDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"mayorAnalitico",
							"fechaAsiento",
							"numeroAsiento",
							"descripcionAsiento",
							"valorDebe",
							"valorHaber",
							"saldoActual",
							"asiento",
							"estadoAsiento",
							"planCuenta",
							"nombreCosto",
							"numeroCentroCosto"};
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleMayorAnalitico> selectByIdMayorAnalitico(Long idMayorAnalitico) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdMayorAnalitico con id de idMayorAnalitico: " + idMayorAnalitico);
		Query query = em.createQuery(" select   b " +
									 " from     DetalleMayorAnalitico b " +
									 " where    b.mayorAnalitico.codigo = :idMayorAnalitico");
		query.setParameter("idMayorAnalitico", idMayorAnalitico);
		return query.getResultList();
	}
}
