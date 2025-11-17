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
package com.saa.basico.ejb;

import java.util.List;
import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *         <p>
 *         Servicio para la administracion de procesos relacionados con
 *         cantidades.
 *         </p>
 */
@Remote
public interface CantidadService {

	/**
	 * Para las unidades
	 */
	String[] UNIDADES = { "", "un ", "dos ", "tres ", "cuatro ", "cinco ",
			"seis ", "siete ", "ocho ", "nueve " };

	/**
	 * Para las decenas
	 */
	String[] DECENAS = { "diez ", "once ", "doce ", "trece ", "catorce ",
			"quince ", "dieciseis ", "diecisiete ", "dieciocho ",
			"diecinueve", "veinte ", "treinta ", "cuarenta ",
			"cincuenta ", "sesenta ", "setenta ", "ochenta ",
			"noventa " };

	/**
	 * Para las centenas
	 */
	String[] CENTENAS = { "", "ciento ", "doscientos ", "trecientos ", "cuatrocientos ",
			"quinientos ", "seiscientos ", "setecientos ", "ochocientos ",
			"novecientos " };

	/**
	 * Veriica si es que un rango de cantidades se traslapa en otros registros.
	 * 
	 * @param fechas      : Listado con cantidades a verificar
	 * @param valorDesde: Cantidad desde
	 * @param valorHasta: Cantidad Hasta
	 * @return : True = existe traslape, False = no existe traslape.
	 * @throws Throwable: Excepcion.
	 */
	boolean verificaTraslape(@SuppressWarnings("rawtypes") List cantidades, Double valorDesde, Double valorHasta)
			throws Throwable;

	/**
	 * Metodo para transformar numeros a letras
	 * 
	 * @param numero      : Numero a transformar.
	 * @param mayusculas: Mayusculas.
	 * @return : Cadena de String.
	 * @throws Throwable: Excepcions.
	 */
	public String Convertir(String numero, boolean mayusculas) throws Throwable;

	/**
	 * Metodo para redondear decimales
	 * 
	 * @param valor            : Valor a redondear.
	 * @param numeroDecimales: Numero de decimales.
	 * @return : Valor redondeado.
	 * @throws Throwable: Excepcions.
	 */
	public Double redondea(Double valor, int numeroDecimales) throws Throwable;

}
