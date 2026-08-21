package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService;
import com.saa.ejb.rhh.service.LiquidacionBeneficioSocialService;
import com.saa.model.rhh.LiquidacionBeneficioSocial;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz LiquidacionBeneficioSocialService.
 *  Contiene los servicios relacionados con la entidad LiquidacionBeneficioSocial.</p>
 */
@Stateless
public class LiquidacionBeneficioSocialServiceImpl implements LiquidacionBeneficioSocialService {

	@EJB
	private LiquidacionBeneficioSocialDaoService liquidacionBeneficioSocialDaoService;

	@Override
	public void save(List<LiquidacionBeneficioSocial> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de liquidacionBeneficioSocial service");
		for (LiquidacionBeneficioSocial registro : lista) {
			liquidacionBeneficioSocialDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de liquidacionBeneficioSocial service");
		//INSTANCIA UNA ENTIDAD
		LiquidacionBeneficioSocial liquidacionBeneficioSocial = new LiquidacionBeneficioSocial();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			liquidacionBeneficioSocialDaoService.remove(liquidacionBeneficioSocial, registro);
		}
	}

	@Override
	public List<LiquidacionBeneficioSocial> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) LiquidacionBeneficioSocial");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<LiquidacionBeneficioSocial> result = liquidacionBeneficioSocialDaoService.selectAll(NombreEntidadesRhh.LIQUIDACION_BENEFICIO_SOCIAL);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de liquidacionBeneficioSocial no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public LiquidacionBeneficioSocial selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de liquidacionBeneficioSocial con id: " + id);
		return liquidacionBeneficioSocialDaoService.selectById(id, NombreEntidadesRhh.LIQUIDACION_BENEFICIO_SOCIAL);
	}

	@Override
	public List<LiquidacionBeneficioSocial> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) LiquidacionBeneficioSocial");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<LiquidacionBeneficioSocial> result = liquidacionBeneficioSocialDaoService.selectByCriteria(datos, NombreEntidadesRhh.LIQUIDACION_BENEFICIO_SOCIAL);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de liquidacionBeneficioSocial no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public LiquidacionBeneficioSocial saveSingle(LiquidacionBeneficioSocial liquidacionBeneficioSocial) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) LiquidacionBeneficioSocial");
		liquidacionBeneficioSocial = liquidacionBeneficioSocialDaoService.save(liquidacionBeneficioSocial, liquidacionBeneficioSocial.getCodigo());
		return liquidacionBeneficioSocial;
	}
}
