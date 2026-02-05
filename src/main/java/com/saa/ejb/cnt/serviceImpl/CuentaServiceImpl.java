package com.saa.ejb.cnt.serviceImpl;

import com.saa.ejb.cnt.service.CuentaService;

import jakarta.ejb.Stateless;

@Stateless
public class CuentaServiceImpl implements CuentaService {

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CuentaService#verificaPrimerNivel(java.lang.String)
	 */
	public Boolean verificaPrimerNivel(String cuenta) throws Throwable {
		System.out.println("Ingresa al metodo verificaPrimerNivel de Cuenta service con cuenta: " + cuenta);
		//VERIFICA SI LA CUENTA TIENE PUNTOS O NO
		return !cuenta.contains(".");
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CuentaService#reversaCaracteres(java.lang.String)
	 */
	public String reversaCaracteres(String cadena) throws Throwable {
		System.out.println("Ingresa al metodo reversaCaracteres de Cuenta service con cadena: " + cadena);
		StringBuffer bufferCadena = new StringBuffer(cadena);
		return bufferCadena.reverse().toString();
	}	

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CuentaService#obtieneIdPadre(java.lang.String)
	 */
	public String obtieneCuentaPadre(String cuenta) throws Throwable {
		System.out.println("Ingresa al metodo obtieneIdPadre de Cuenta service con cuenta: " + cuenta);
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
	
}
