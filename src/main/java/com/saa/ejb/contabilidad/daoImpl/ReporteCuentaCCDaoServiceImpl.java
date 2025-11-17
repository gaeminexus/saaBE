package com.saa.ejb.contabilidad.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.ReporteCuentaCCDaoService;
import com.saa.model.contabilidad.ReporteCuentaCC;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Stateless
public class ReporteCuentaCCDaoServiceImpl extends EntityDaoImpl<ReporteCuentaCC>  implements ReporteCuentaCCDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"planCuenta",
							"nombreCuenta",
							"numeroCuenta",
							"secuencia",
							"saldoAnterio",
							"debe",
							"haber",
							"saldoActual"};
	}
	
}