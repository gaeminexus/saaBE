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

import com.saa.basico.ejb.JerarquiaDaoService;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.model.scp.Jerarquia;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft. Clase JerarquiaDaoImpl.
 */
@Stateless
public class JerarquiaDaoServiceImpl extends EntityDaoImpl<Jerarquia> implements JerarquiaDaoService {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[] { "codigo",
				"nombre",
				"nivel",
				"codigoPadre",
				"descripcion",
				"ultimoNivel",
				"rubroTipoEstructuraP",
				"rubroTipoEstructuraH",
				"codigoAlterno",
				"rubroNivelCaracteristicaP",
				"rubroNivelCaracteristicaH",
				"empresas" };
	}

}
