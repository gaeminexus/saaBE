package com.saa.ejb.contabilidad.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.DetalleReporteCuentaCCDaoService;
import com.saa.model.cnt.DetalleReporteCuentaCC;

import jakarta.ejb.Stateless;

@Stateless
public class DetalleReporteCuentaCCDaoServiceImpl extends EntityDaoImpl<DetalleReporteCuentaCC>  implements DetalleReporteCuentaCCDaoService{
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"reporteCuentaCC",
							"centroCosto",
							"nombreCosto",
							"numeroCosto",
							"debe",
							"haber"};
	}
}
