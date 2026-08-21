package com.saa.ejb.rhh.service;

import com.saa.model.rhh.PlanillaControlIess;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Arma la planilla de control del IESS de un periodo.</p>
 *
 * <p><b>Es un control, no un documento oficial.</b> La planilla que se paga la genera el
 * IESS a partir de la historia laboral del afiliado; esta se construye desde nuestras
 * nominas para enfrentarla contra aquella <b>antes de transferir</b>. Es la version en
 * linea del bloque 3 de <code>CONTRASTE_MES_CONTRA_ROL_REAL.sql</code>, que lleva tres
 * meses funcionando, mas el comprobante completo.</p>
 *
 * <p>Sirve para algo muy concreto y ya ocurrido: en marzo de 2026 el portal declaro a dos
 * personas que ya no estaban --208,22 de mas-- porque nadie registro el aviso de salida.
 * Contra esta planilla la diferencia se habria visto antes de pagar.</p>
 */
@Local
public interface PlanillaControlIessService {

	/**
	 * Construye la planilla de control del periodo.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: La planilla, con sus lineas, totales y avisos
	 * @throws Throwable	: IncomeException si el periodo no existe o no esta calculado
	 */
	PlanillaControlIess generar(Long idPeriodo) throws Throwable;

}
