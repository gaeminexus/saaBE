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
package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro TipoGrupoProductos (74)
 */
public interface TipoGrupoProductos {

	// Ids de los elementos hijos
	public static final int RAIZ = 0;
	public static final int BIEN = 1;
	public static final int SERVICIO = 2;
	/**
	 * Grupo especial de uso exclusivo del backend.
	 * Productos auto-creados desde carga de XML que aún no tienen grupo definitivo.
	 * No puede ser creado, modificado ni eliminado desde el frontend.
	 */
	public static final int POR_CLASIFICAR = 3;

}