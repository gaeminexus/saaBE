package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Direccion;

import jakarta.ejb.Local;

@Local
public interface DireccionDaoService extends EntityDao<Direccion> {
	
	
	/** Select dirección by parent idIdentidad
	 * @param idIdentidad
	 * @return lista de direcciones
	 */
	List<Direccion> selectByParent(Long idIdentidad);

	/**
	 * Domicilio vigente de una entidad (DISENO-DIRECCION-DOMICILIO-UNICA-Y-CIUDAD.md §2): entre las direcciones activas,
	 * no de trabajo y que no estén en CRD.DRTR, la de porDefecto = 1 de MENOR código; si ninguna está por defecto, la de
	 * menor código. Es la única regla para pantalla, solicitud, pagaré y contrato de adhesión.
	 * @return la dirección, o null si la entidad no tiene domicilio
	 */
	Direccion selectDomicilioVigente(Long idEntidad);

	/** Domicilios activos (no de trabajo, fuera de CRD.DRTR) de la entidad marcados por defecto. */
	List<Direccion> selectDomiciliosPorDefecto(Long idEntidad);

}
