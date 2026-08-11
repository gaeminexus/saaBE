package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ParticipeXCargaArchivo;

import jakarta.ejb.Local;

@Local
public interface ParticipeXCargaArchivoDaoService extends EntityDao<ParticipeXCargaArchivo>{
	
	/**
	 * Busca un partícipe por código Petro y código de producto dentro de una carga específica.
	 * Usado para relacionar PH con HS (el seguro viene separado en HS).
	 * 
	 * @param codigoPetro Código Petro del partícipe
	 * @param codigoProducto Código del producto (ej: "HS")
	 * @param codigoCargaArchivo Código de la carga archivo
	 * @return ParticipeXCargaArchivo encontrado o null
	 * @throws Throwable Si ocurre algún error
	 */
	ParticipeXCargaArchivo selectByCodigoPetroYProductoEnCarga(Long codigoPetro, String codigoProducto, Long codigoCargaArchivo) throws Throwable;
	
	/**
	 * Busca todos los partícipes asociados a un DetalleCargaArchivo específico
	 * 
	 * @param codigoDetalleCargaArchivo Código del DetalleCargaArchivo
	 * @return Lista de ParticipeXCargaArchivo encontrados
	 */
	List<ParticipeXCargaArchivo> selectByDetalleCargaArchivo(Long codigoDetalleCargaArchivo);

	/**
	 * Suma el valor efectivamente descontado a un rol Petro, para un producto,
	 * en el periodo de afectación indicado.
	 *
	 * Considera todas las cargas no anuladas de ese mes/año, porque un mismo
	 * periodo puede tener más de una carga (una por filial).
	 *
	 * @param codigoPetro    Rol Petro del partícipe (PXCACDPT)
	 * @param codigoProducto Código Petro del producto, sin espacios (ej: "AH")
	 * @param anioAfectacion Año de afectación de la carga
	 * @param mesAfectacion  Mes de afectación de la carga
	 * @return Suma de totalDescontado; 0.0 si no hay registros en ese periodo
	 * @throws Throwable Si ocurre algún error
	 */
	Double sumaDescontadoPorProductoYPeriodo(Long codigoPetro, String codigoProducto,
			Long anioAfectacion, Long mesAfectacion) throws Throwable;

	/**
	 * Indica si existe alguna carga no anulada para el periodo indicado que
	 * contenga el producto solicitado.
	 *
	 * Sirve para distinguir "el partícipe no aportó" de "todavía no se ha
	 * cargado ese periodo", que no es lo mismo y no debe generar mora.
	 *
	 * @param codigoProducto Código Petro del producto, sin espacios (ej: "AH")
	 * @param anioAfectacion Año de afectación de la carga
	 * @param mesAfectacion  Mes de afectación de la carga
	 * @return true si el periodo tiene al menos una carga con ese producto
	 * @throws Throwable Si ocurre algún error
	 */
	boolean existeCargaConProductoEnPeriodo(String codigoProducto,
			Long anioAfectacion, Long mesAfectacion) throws Throwable;

}

