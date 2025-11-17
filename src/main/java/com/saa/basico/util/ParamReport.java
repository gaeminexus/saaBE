package com.saa.basico.util;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ParamReport implements Serializable {

	private String campo = null;
	private String valor = null;
	private int tipoDato;

	public String getCampo() {
		return campo;
	}

	public void setCampo(String campo) {
		this.campo = campo;
	}

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}

	public int getTipoDato() {
		return tipoDato;
	}

	public void setTipoDato(int tipoDato) {
		this.tipoDato = tipoDato;
	}

}
