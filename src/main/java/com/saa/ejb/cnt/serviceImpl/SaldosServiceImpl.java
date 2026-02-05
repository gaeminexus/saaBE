package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.service.SaldosService;
import com.saa.model.cnt.Saldos;

import jakarta.ejb.Stateless;

@Stateless
public class SaldosServiceImpl implements SaldosService {

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.SaldosService#verificaPrimerNivel(java.lang.String)
	 */
	public Boolean verificaPrimerNivel(String cuenta) throws Throwable {
		System.out.println("Ingresa al metodo verificaPrimerNivel de Saldos service con cuenta: " + cuenta);
		//VERIFICA SI LA CUENTA TIENE PUNTOS O NO
		return !cuenta.contains(".");
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.SaldosService#reversaCaracteres(java.lang.String)
	 */
	public String reversaCaracteres(String cadena) throws Throwable {
		System.out.println("Ingresa al metodo reversaCaracteres de Saldos service con cadena: " + cadena);
		StringBuffer bufferCadena = new StringBuffer(cadena);
		return bufferCadena.reverse().toString();
	}	

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.SaldosService#obtieneIdPadre(java.lang.String)
	 */
	public String obtieneSaldosPadre(String cuenta) throws Throwable {
		System.out.println("Ingresa al metodo obtieneIdPadre de Saldos service con cuenta: " + cuenta);
		String cuentaPadre = null;
		// REVERSA LA CADENA
		String cuentaReversada = reversaCaracteres(cuenta);
		int cuentaSize = cuenta.length();
		// CUENTA NUMERO DE CARACTERES DESDE EL ULTIMO PUNTO
		int ultimoNivelSize = cuentaReversada.indexOf('.') + 1;
		if (ultimoNivelSize > 0) {
			cuentaPadre = cuenta.substring(0, cuentaSize - ultimoNivelSize);
		}else{
			cuentaPadre = "0";
		} 

		return cuentaPadre;
	}

	@Override
	public Saldos saveSingle(Saldos registro) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object selectByCriteria(List<DatosBusqueda> registros) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object selectByCriteria1(List<DatosBusqueda> registros) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
