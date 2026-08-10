package com.saa.ejb.cxp.service;

import java.util.List;

import com.saa.model.cxp.LotePago;

/**
 * Lee el archivo de respuesta que devuelve la entidad financiera, indicando qué
 * transferencias del lote se ejecutaron y cuáles no.
 *
 * Igual que el formateador de salida, el formato lo define el banco y por eso se
 * aísla en esta interfaz.
 *
 * PENDIENTE: el formato oficial del archivo de respuesta todavía no está
 * definido. La implementación actual ({@code LectorRespuestaBancoExcelImpl})
 * asume un Excel con columnas por posición y es provisional.
 */
public interface LectorRespuestaBanco {

	/**
	 * Interpreta el archivo de respuesta del banco.
	 * @param archivo : Contenido binario del archivo cargado
	 * @param lote    : Lote al que corresponde la respuesta
	 * @return        : Una respuesta por cada pago reportado por el banco
	 * @throws Throwable : Excepcion si el archivo no se puede interpretar
	 */
	List<RespuestaPagoBanco> leer(byte[] archivo, LotePago lote) throws Throwable;
}
