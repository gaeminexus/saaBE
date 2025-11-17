package com.saa.ejb.contabilidad.daoImpl;


import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.ReporteContableDaoService;
import com.saa.model.contabilidad.ReporteContable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ReporteContableDaoServiceImpl extends EntityDaoImpl<ReporteContable>  implements ReporteContableDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"empresa",
							"nombreReporte",
							"estado",
							"codigoAlterno",
							"detalleReporteContables"};
	}

}