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
	
	// Novedades de procesamiento FASE 2
	public static final int PRODUCTO_NO_MAPEADO = 9;
	public static final int PRESTAMO_NO_ENCONTRADO = 10;
	public static final int MULTIPLES_PRESTAMOS_ACTIVOS = 11;
	public static final int CUOTA_NO_ENCONTRADA = 12;
	public static final int MONTO_INCONSISTENTE = 13;
	public static final int PRESTAMO_PROCESADO_OK = 14;
	public static final int APORTE_GENERADO_OK = 15;
	public static final int CUOTA_FECHA_DIFERENTE = 16; // Cuota encontrada pero con fecha diferente al mes del archivo
	public static final int DIFERENCIA_MENOR_UN_DOLAR = 17; // Diferencia menor a $1 en el monto (dentro de tolerancia)
	
}
