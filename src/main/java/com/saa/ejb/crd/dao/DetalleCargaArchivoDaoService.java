package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DetalleCargaArchivo;

import jakarta.ejb.Local;

@Local
public interface DetalleCargaArchivoDaoService extends EntityDao<DetalleCargaArchivo>{

	/**
	 * Busca todos los detalles asociados a una CargaArchivo específica
	 * 
	 * @param codigoCargaArchivo Código de la CargaArchivo
	 * @return Lista de DetalleCargaArchivo encontrados
	 */
	List<DetalleCargaArchivo> selectByCargaArchivo(Long codigoCargaArchivo);

}
