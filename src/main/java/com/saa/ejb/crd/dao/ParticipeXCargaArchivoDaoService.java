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
	 * Todas las filas de un partícipe (rol Petro) dentro de una carga, TODOS los productos —
	 * VALIDACION-TOPE-AFECTACION-MANUAL.md §8: es la fuente de "disponible" del endpoint de
	 * tope, acotada a un solo partícipe (no escanea la carga completa como el método de
	 * validación, que sí necesita verlos a todos de una vez).
	 *
	 * @param codigoPetro        : Rol Petro del partícipe
	 * @param codigoCargaArchivo : Código de la carga (CRD.CRAR)
	 * @return                   : Lista; VACÍA si el partícipe no tiene filas en esa carga
	 * @throws Throwable         : Excepcion
	 */
	List<ParticipeXCargaArchivo> selectByCodigoPetroEnCarga(Long codigoPetro, Long codigoCargaArchivo) throws Throwable;

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

