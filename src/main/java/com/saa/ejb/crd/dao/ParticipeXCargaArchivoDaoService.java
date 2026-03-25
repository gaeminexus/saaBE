package com.saa.ejb.crd.dao;

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
			
}
