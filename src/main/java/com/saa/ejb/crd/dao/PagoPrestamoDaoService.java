package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.PagoPrestamo;

import jakarta.ejb.Local;


@Local
public interface PagoPrestamoDaoService extends EntityDao<PagoPrestamo> {

	/**
	 * Busca todos los pagos asociados a un DetallePrestamo específico
	 * 
	 * @param codigoDetallePrestamo Código del DetallePrestamo
	 * @return Lista de PagoPrestamo encontrados (vacía si no hay registros)
	 */
	List<PagoPrestamo> selectByIdDetallePrestamo(Long codigoDetallePrestamo);

}
