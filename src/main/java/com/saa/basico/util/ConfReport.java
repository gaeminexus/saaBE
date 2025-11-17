package com.saa.basico.util;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ConfReport implements Serializable {

	private String nombre;
	private ParamReport[] parametros;
	private String extension;

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public ParamReport[] getParametros() {
		return parametros;
	}

	public void setParametros(ParamReport[] parametros) {
		this.parametros = parametros;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

}
