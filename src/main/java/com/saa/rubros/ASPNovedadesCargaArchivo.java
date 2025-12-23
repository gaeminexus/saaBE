package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro Novedades carga archivo (169)
 */
public interface ASPNovedadesCargaArchivo {
	
	// Ids de los elementos hijos
	public static final int OK = 0;
	public static final int PARTICIPE_NO_ENCONTRADO = 1;
	public static final int CODIGO_ROL_DUPLICADO = 2;
	public static final int NOMBRE_ENTIDAD_DUPLICADO = 3;
	public static final int CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE = 4;
	
	// Novedades financieras
	public static final int SIN_DESCUENTOS = 5;
	public static final int DESCUENTOS_INCOMPLETOS = 6;
	public static final int DESCUENTOS_ADICIONALES = 7;
	public static final int VALORES_CERO = 8;
	
	
}
