package com.saa.ejb.contabilidad.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.MatchCuentaDaoService;
import com.saa.model.cnt.MatchCuenta;

import jakarta.ejb.Stateless;

@Stateless
public class MatchCuentaDaoServiceImpl extends EntityDaoImpl<MatchCuenta>  implements MatchCuentaDaoService{

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"empresaOrigen",
							"cuentaOrigen",
							"empresaDestino",
							"cuentaDestino",
							"estado"};
	}
	
}
