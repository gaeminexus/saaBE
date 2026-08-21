package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.LiquidacionBeneficioSocial;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad LiquidacionBeneficioSocial.
 *  Accede a los metodos DAO y procesa los datos para el LiquidacionBeneficioSocial.</p>
 */
@Local
public interface LiquidacionBeneficioSocialService extends EntityService<LiquidacionBeneficioSocial> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	LiquidacionBeneficioSocial selectById(Long id) throws Throwable;

}
