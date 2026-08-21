package com.saa.ejb.rhh.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.FormatoArchivoMarcacionDaoService;
import com.saa.model.rhh.FormatoArchivoMarcacion;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion FormatoArchivoMarcacionDaoService.
 */
@Stateless
public class FormatoArchivoMarcacionDaoServiceImpl extends EntityDaoImpl<FormatoArchivoMarcacion> implements FormatoArchivoMarcacionDaoService {

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.FormatoArchivoMarcacionDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) FormatoArchivoMarcacion");
		return new String[]{"codigo",
							"empresa",
							"nombre",
							"marca",
							"tipoFormato",
							"delimitador",
							"lineasCabecera",
							"lineasPie",
							"formatoFecha",
							"formatoHora",
							"formatoFechaHora",
							"codificacion",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

}
